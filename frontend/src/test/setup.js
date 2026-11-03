import '@testing-library/jest-dom';

// Node >= 25 expone un `localStorage`/`sessionStorage` global que vale `undefined`
// si no se pasa --localstorage-file, y eso tapa el Storage de jsdom (en Vitest,
// `window === globalThis`). Solo cuando el global no es utilizable se instala
// un Storage en memoria con la misma interfaz que usa la aplicación.
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
