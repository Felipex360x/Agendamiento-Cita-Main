export default function displayText(value) {
  if (typeof value !== 'string' || !/Ã|Â/.test(value) || [...value].some((character) => character.charCodeAt(0) > 0xff)) {
    return value;
  }

  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(Uint8Array.from(value, (character) => character.charCodeAt(0)));
  } catch {
    return value;
  }
}
