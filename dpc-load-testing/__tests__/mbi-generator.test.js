import { generateMBI } from "../utils/mbi-generator";

function isArrayUnique(arr) {
  return Array.isArray(arr) && new Set(arr).size === arr.length;
}

describe('generateMBI', () => {
  test('generates an 11 character string', () => {
    expect(generateMBI(0)).toHaveLength(11);
  });

  test('generates strings with the correct character placement', () => {
    const mbis = [];
    for (let i = 0; i <= 1000; i++){
      mbis.push(generateMBI(i));
    }
    mbis.every(result => {
      expect(result[0]).toMatch(/^[1-9]$/); // first character is a number 1-9
      expect(result[1]).toMatch(/[A-Z]/); // second character is an uppercase letter
      expect(result[2]).toMatch(/^[0-9A-Z]+$/); // third character is alphanumeric
      expect(result[3]).toMatch(/^[0-9]$/); // fourth character is a number 0-9
      expect(result[4]).toMatch(/[A-Z]/); // fifth character is an uppercase letter
      expect(result[5]).toMatch(/^[0-9A-Z]+$/); // sixth character is alphanumeric
      expect(result[6]).toMatch(/^[0-9]$/); // seventh character is a number 0-9
      expect(result[7]).toMatch(/[A-Z]/); // eighth character is an uppercase letter
      expect(result[8]).toMatch(/[A-Z]/); // ninth character is an uppercase letter
      expect(result[9]).toMatch(/^[0-9]$/); // tenth character is a number 0-9
      expect(result[10]).toMatch(/^[0-9]$/); // eleventh character is a number 0-9
    });
  });

  test('generates only unique MBIs', () => {
    const mbis = [];
    for (let i = 0; i <= 1000; i++){
      mbis.push(generateMBI(i));
    }

    expect(isArrayUnique(mbis)).toBeTruthy();
  });
});
