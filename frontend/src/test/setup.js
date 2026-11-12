import '@testing-library/jest-dom';

// En versiones nuevas de Node, `localStorage` y `sessionStorage` pueden venir vacíos
// si no se indica un archivo, y eso impide que funcionen en las pruebas.
// Si no están listos para usarse, se crea una versión simple en memoria para los tests.
class MemoryStorage {
  #data = new Map();
  get length() { return this.#data.size; }
  key(index) { return [...this.#data.keys()][index] ?? null; }
  getItem(key) { return this.#data.has(String(key)) ? this.#data.get(String(key)) : null; }
  setItem(key, value) { this.#data.set(String(key), String(value)); }
  removeItem(key) { this.#data.delete(String(key)); }
  clear() { this.#data.clear(); }
}

for (const name of ['localStorage', 'sessionStorage']) {
  if (typeof globalThis[name]?.clear !== 'function') {
    const storage = new MemoryStorage();
    Object.defineProperty(globalThis, name, { configurable: true, get: () => storage });
  }
}
