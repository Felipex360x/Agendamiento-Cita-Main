import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

const frontendDir = path.resolve(__dirname, '../..');
const projectRootDir = path.resolve(frontendDir, '..');
const TEXT_EXTENSIONS = ['.html', '.js', '.jsx', '.css', '.md'];

function walkDir(dir, fileList = []) {
  if (!fs.existsSync(dir)) return fileList;
  for (const file of fs.readdirSync(dir)) {
    const fullPath = path.join(dir, file);
    if (fs.statSync(fullPath).isDirectory()) {
      walkDir(fullPath, fileList);
    } else {
      fileList.push(fullPath);
    }
  }
  return fileList;
}

// Archivos con texto visible para el usuario: la aplicación React, su HTML de entrada,
// las plantillas de correo del backend y el README.
const htmlEntryFiles = [
  path.join(frontendDir, 'index.html'),
  ...walkDir(path.join(projectRootDir, 'src/main/resources/templates')),
].filter((file) => path.extname(file).toLowerCase() === '.html');

const auditedFiles = [
  ...walkDir(path.join(frontendDir, 'src')).filter((file) => !file.includes(`${path.sep}__tests__${path.sep}`)),
  ...htmlEntryFiles,
  path.join(projectRootDir, 'README.md'),
].filter((file) => TEXT_EXTENSIONS.includes(path.extname(file).toLowerCase()));

const relative = (file) => path.relative(projectRootDir, file);

describe('Integridad de caracteres (mojibake)', () => {
  it('encuentra archivos para auditar', () => {
    expect(auditedFiles.length).toBeGreaterThan(20);
    expect(htmlEntryFiles.length).toBeGreaterThan(1);
  });

  it('ningún archivo contiene el carácter de reemplazo Unicode U+FFFD', () => {
    const corrupted = [];
    for (const file of auditedFiles) {
      fs.readFileSync(file, 'utf8').split('\n').forEach((line, index) => {
        if (line.includes('�')) corrupted.push({ file: relative(file), line: index + 1 });
      });
    }
    expect(corrupted).toEqual([]);
  });

  it('ningún archivo presenta doble codificación UTF-8 (Ã¡, Ã©, Ã³, etc.)', () => {
    const doubleUtf8Pattern = /Ã[¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ]/g;
    const corrupted = [];
    for (const file of auditedFiles) {
      const matches = fs.readFileSync(file, 'utf8').match(doubleUtf8Pattern);
      if (matches) corrupted.push({ file: relative(file), samples: matches.slice(0, 5) });
    }
    expect(corrupted).toEqual([]);
  });

  it('ningún archivo contiene glifos corruptos (ǭ, Ǹ) ni palabras mutiladas (Duracin, conexin)', () => {
    const badPatterns = [/ǭ/, /Ǹ/, /\bDuracin\b/, /\bconexin\b/];
    const findings = [];
    for (const file of auditedFiles) {
      const content = fs.readFileSync(file, 'utf8');
      for (const pattern of badPatterns) {
        if (pattern.test(content)) findings.push({ file: relative(file), pattern: String(pattern) });
      }
    }
    expect(findings).toEqual([]);
  });

  it('todos los HTML declaran explícitamente <meta charset="UTF-8">', () => {
    const missingCharset = htmlEntryFiles
      .filter((file) => !/<meta\s+charset=["']?utf-8["']?/i.test(fs.readFileSync(file, 'utf8')))
      .map(relative);
    expect(missingCharset).toEqual([]);
  });
});
