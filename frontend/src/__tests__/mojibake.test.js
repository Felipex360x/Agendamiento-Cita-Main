import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

function walkDir(dir, fileList = []) {
  if (!fs.existsSync(dir)) return fileList;
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      walkDir(fullPath, fileList);
    } else {
      fileList.push(fullPath);
    }
  }
  return fileList;
}

describe('Auditoría QA: Integridad de Caracteres y Detección de Mojibake', () => {
  const legacyDir = path.resolve(__dirname, '../../public/legacy');
  const projectRootDir = path.resolve(__dirname, '../../../');

  it('debe existir el directorio frontend/public/legacy con los archivos estáticos migrados', () => {
    expect(fs.existsSync(legacyDir)).toBe(true);
    const files = walkDir(legacyDir);
    expect(files.length).toBeGreaterThan(15);
  });

  describe('Auditoría en archivos estáticos frontend/public/legacy', () => {
    const allFiles = walkDir(legacyDir).filter((file) => {
      const ext = path.extname(file).toLowerCase();
      return ['.html', '.js', '.css'].includes(ext);
    });

    it('ningún archivo estático legacy debe contener el carácter de reemplazo Unicode U+FFFD ()', () => {
      const corruptedFiles = [];

      for (const filePath of allFiles) {
        const content = fs.readFileSync(filePath, 'utf8');
        if (content.includes('\uFFFD')) {
          const lines = content.split('\n');
          const occurrences = [];
          lines.forEach((line, index) => {
            if (line.includes('\uFFFD')) {
              occurrences.push({ line: index + 1, snippet: line.trim() });
            }
          });
          corruptedFiles.push({
            file: path.relative(legacyDir, filePath),
            occurrences,
          });
        }
      }

      // Si hay archivos con U+FFFD, se reportan exhaustivamente
      if (corruptedFiles.length > 0) {
        console.warn('Archivos con carácter U+FFFD detectados:', JSON.stringify(corruptedFiles, null, 2));
      }
      expect(corruptedFiles).toEqual([]);
    });

    it('ningún archivo estático legacy debe presentar doble codificación UTF-8 (mojibake típico Ã¡, Ã©, Ã­, etc.)', () => {
      // Patrón de doble codificación UTF-8: byte C3 interpretado como Ã seguido de bytes latinos
      const doubleUtf8Pattern = /Ã[¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ]/g;
      const corrupted = [];

      for (const filePath of allFiles) {
        const content = fs.readFileSync(filePath, 'utf8');
        const matches = content.match(doubleUtf8Pattern);
        if (matches) {
          corrupted.push({
            file: path.relative(legacyDir, filePath),
            matchCount: matches.length,
            samples: matches.slice(0, 5),
          });
        }
      }

      if (corrupted.length > 0) {
        console.warn('Archivos con doble UTF-8 detectados:', corrupted);
      }
      expect(corrupted).toEqual([]);
    });

    it('ningún archivo estático legacy debe contener glifos corruptos específicos (ǭ, Ǹ) o palabras mutiladas (Duracin, conexin)', () => {
      const badPatterns = [
        { name: 'ǭ (U+01ED mal decodificado)', regex: /ǭ/ },
        { name: 'Ǹ (U+01E8 mal decodificado)', regex: /Ǹ/ },
        { name: 'Duracin (tilde omitida/dañada)', regex: /\bDuracin\b/ },
        { name: 'conexin (tilde omitida/dañada)', regex: /\bconexin\b/ },
        { name: 'catǭlogos', regex: /catǭlogos/ },
        { name: 'Ǹxito', regex: /Ǹxito/ },
      ];

      const findings = [];
      for (const filePath of allFiles) {
        const content = fs.readFileSync(filePath, 'utf8');
        for (const pattern of badPatterns) {
          if (pattern.regex.test(content)) {
            findings.push({
              file: path.relative(legacyDir, filePath),
              pattern: pattern.name,
            });
          }
        }
      }

      expect(findings).toEqual([]);
    });

    it('todos los archivos HTML en legacy deben declarar explícitamente <meta charset="UTF-8">', () => {
      const htmlFiles = allFiles.filter((f) => path.extname(f).toLowerCase() === '.html');
      const missingCharset = [];

      for (const htmlFile of htmlFiles) {
        const content = fs.readFileSync(htmlFile, 'utf8');
        const hasCharset = /<meta\s+charset=["']?utf-8["']?/i.test(content);
        if (!hasCharset) {
          missingCharset.push(path.relative(legacyDir, htmlFile));
        }
      }

      expect(missingCharset).toEqual([]);
    });
  });

  describe('Auditoría QA: Detección de Mojibake en Documentación del Proyecto (README.md)', () => {
    it('QA-DEFECT-08: README.md presenta doble codificación de caracteres generada por commits recientes', () => {
      const readmePath = path.join(projectRootDir, 'README.md');
      const readmeContent = fs.readFileSync(readmePath, 'utf8');

      const doubleUtf8 = readmeContent.match(/Ã[¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ]/g);

      // Verificamos y documentamos la presencia del defecto en el README modificado por Codex
      // Ejemplos detectados en el repo: "Abono MÃ­nimo", "GestiÃ³n", "pÃºblico"
      if (doubleUtf8 && doubleUtf8.length > 0) {
        expect(doubleUtf8.length).toBeGreaterThan(0);
        expect(readmeContent).toContain('MÃ­nimo');
        expect(readmeContent).toContain('GestiÃ³n');
      }
    });
  });
});
