import { generateMBI } from "../utils/mbi-generator";
import { isArrayUnique } from "../utils/test-utils";

describe('generateMBI', () => {
  const mbis = [];
  beforeAll(() => {
    for (let i = 0; i <= 10000; i++){
      mbis.push(generateMBI(i));
    }
  });

  test('generates an 11 character string', () => {
    mbis.every(mbi => {
      expect(mbi).toHaveLength(11);
    })
  });

  test('generates strings with the correct character placement', () => {
    mbis.every(mbi => {
      expect(mbi[0]).toMatch(/^[1-9]$/); // first character is a number 1-9
      expect(mbi[1]).toMatch(/[A-Z]/); // second character is an uppercase letter
      expect(mbi[2]).toMatch(/^[0-9A-Z]+$/); // third character is alphanumeric
      expect(mbi[3]).toMatch(/^[0-9]$/); // fourth character is a number 0-9
      expect(mbi[4]).toMatch(/[A-Z]/); // fifth character is an uppercase letter
      expect(mbi[5]).toMatch(/^[0-9A-Z]+$/); // sixth character is alphanumeric
      expect(mbi[6]).toMatch(/^[0-9]$/); // seventh character is a number 0-9
      expect(mbi[7]).toMatch(/[A-Z]/); // eighth character is an uppercase letter
      expect(mbi[8]).toMatch(/[A-Z]/); // ninth character is an uppercase letter
      expect(mbi[9]).toMatch(/^[0-9]$/); // tenth character is a number 0-9
      expect(mbi[10]).toMatch(/^[0-9]$/); // eleventh character is a number 0-9
    });
  });

  test('generates only unique MBIs', () => {
    expect(isArrayUnique(mbis)).toBeTruthy();
  });
});
