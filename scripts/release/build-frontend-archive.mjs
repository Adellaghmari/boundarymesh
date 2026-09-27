import { createHash } from "node:crypto";
import { existsSync, lstatSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { basename, dirname, join, posix, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { gzipSync } from "node:zlib";

const TAR_BLOCK_SIZE = 512;
const ARCHIVE_ROOT = "frontend-dist";

function writeString(header, offset, length, value) {
    const bytes = Buffer.from(value, "utf8");
    if (bytes.length > length) {
        throw new Error(`TAR field is too long: ${value}`);
    }
    bytes.copy(header, offset);
}

function writeOctal(header, offset, length, value) {
    const encoded = value.toString(8).padStart(length - 1, "0") + "\0";
    writeString(header, offset, length, encoded);
}

function splitTarPath(archivePath) {
    if (Buffer.byteLength(archivePath) <= 100) {
        return { name: archivePath, prefix: "" };
    }
    for (let index = archivePath.lastIndexOf("/"); index > 0; index = archivePath.lastIndexOf("/", index - 1)) {
        const prefix = archivePath.slice(0, index);
        const name = archivePath.slice(index + 1);
        if (Buffer.byteLength(prefix) <= 155 && Buffer.byteLength(name) <= 100) {
            return { name, prefix };
        }
    }
    throw new Error(`Archive path cannot be represented by ustar: ${archivePath}`);
}

function createFileHeader(archivePath, size, sourceDateEpoch) {
    const header = Buffer.alloc(TAR_BLOCK_SIZE);
    const { name, prefix } = splitTarPath(archivePath);
    writeString(header, 0, 100, name);
    writeOctal(header, 100, 8, 0o644);
    writeOctal(header, 108, 8, 0);
    writeOctal(header, 116, 8, 0);
    writeOctal(header, 124, 12, size);
    writeOctal(header, 136, 12, sourceDateEpoch);
    header.fill(0x20, 148, 156);
    writeString(header, 156, 1, "0");
    writeString(header, 257, 6, "ustar\0");
    writeString(header, 263, 2, "00");
    writeString(header, 345, 155, prefix);
    const checksum = header.reduce((sum, byte) => sum + byte, 0);
    writeString(header, 148, 8, checksum.toString(8).padStart(6, "0") + "\0 ");
    return header;
}

function listFiles(inputDirectory) {
    const files = [];
    function walk(directory) {
        const entries = readdirSync(directory, { withFileTypes: true }).sort((a, b) =>
            a.name.localeCompare(b.name, "en"),
        );
        for (const entry of entries) {
            const absolutePath = join(directory, entry.name);
            const stat = lstatSync(absolutePath);
            if (stat.isSymbolicLink()) {
                throw new Error(`Symbolic links are not allowed in the frontend release: ${absolutePath}`);
            }
            if (stat.isDirectory()) {
                walk(absolutePath);
            } else if (stat.isFile()) {
                files.push(absolutePath);
            } else {
                throw new Error(`Unsupported frontend release entry: ${absolutePath}`);
            }
        }
    }
    walk(inputDirectory);
    return files;
}

export function buildDeterministicFrontendArchive({ inputDirectory, outputFile, sourceDateEpoch }) {
    const input = resolve(inputDirectory);
    const output = resolve(outputFile);
    if (!existsSync(input) || !lstatSync(input).isDirectory()) {
        throw new Error(`Frontend distribution directory does not exist: ${input}`);
    }
    if (!Number.isSafeInteger(sourceDateEpoch) || sourceDateEpoch < 0) {
        throw new Error("SOURCE_DATE_EPOCH must be a non-negative integer.");
    }
    if (existsSync(output) || existsSync(`${output}.sha256`)) {
        throw new Error(`Refusing to overwrite immutable release output: ${output}`);
    }

    const chunks = [];
    for (const file of listFiles(input)) {
        const relativePath = relative(input, file).split(sep).join(posix.sep);
        const archivePath = posix.join(ARCHIVE_ROOT, relativePath);
        const content = readFileSync(file);
        chunks.push(createFileHeader(archivePath, content.length, sourceDateEpoch), content);
        const remainder = content.length % TAR_BLOCK_SIZE;
        if (remainder !== 0) {
            chunks.push(Buffer.alloc(TAR_BLOCK_SIZE - remainder));
        }
    }
    chunks.push(Buffer.alloc(TAR_BLOCK_SIZE * 2));

    const archive = gzipSync(Buffer.concat(chunks), { level: 9, mtime: 0 });
    const sha256 = createHash("sha256").update(archive).digest("hex");
    mkdirSync(dirname(output), { recursive: true });
    writeFileSync(output, archive, { flag: "wx" });
    writeFileSync(`${output}.sha256`, `${sha256}  ${basename(output)}\n`, { flag: "wx" });
    return { sha256, size: archive.length, entryCount: listFiles(input).length };
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) {
    const inputDirectory = option("--input");
    const outputFile = option("--output");
    const epochText = option("--source-date-epoch") ?? process.env.SOURCE_DATE_EPOCH;
    if (!inputDirectory || !outputFile || !epochText || !/^\d+$/.test(epochText)) {
        throw new Error(
            "Usage: node build-frontend-archive.mjs --input <dist> --output <frontend-dist.tar.gz> --source-date-epoch <epoch>",
        );
    }
    const result = buildDeterministicFrontendArchive({
        inputDirectory,
        outputFile,
        sourceDateEpoch: Number(epochText),
    });
    process.stdout.write(`frontend archive sha256:${result.sha256}\n`);
}
