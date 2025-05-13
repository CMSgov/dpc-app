/**
 * Imported from https://github.com/EDumdum/luhn, available as `luhn-js` on NPM.
 * 
 * Available under MIT License: https://github.com/EDumdum/luhn/blob/master/LICENSE
 */

'use strict';

const luhn = {
    /**
     * Check requirements.  
     * Returns if the Luhn check digit is valid.
     *
     * Requirements:
     * - rawValue must be not `Null`
     * - rawValue must be of type `String`
     * - rawValue must respect format `^[0-9]{2,}$`
     * 
     * @param {*} rawValue 
     */
    isValid: function(rawValue) {
        const value = stringifyInput(rawValue);

        if (!value.match(FORMAT_ISVALID)) {
            throw new Error('Exception value of format \'' + FORMAT_ISVALID + '\', found: \'' + value + '\'');
        }
        
        return getLuhnRemainder(value) === 0;
    },

    /**
     * Check requirements.  
     * Returns the Luhn check digit appended to the value.
     * 
     * Requirements:
     * - rawValue must be not `Null`
     * - rawValue must be of type `String`
     * - rawValue must respest format `^[0-9]{1,}$`
     * 
     * @param {*} rawValue 
     */
    generate: function(rawValue) {
        const value = stringifyInput(rawValue);

        if (!value.match(FORMAT_GENERATE)) {
            throw new Error('Exception value of format \'' + FORMAT_GENERATE + '\', found: \'' + value + '\'');
        }

        return value + ((10 - getLuhnRemainder(value + '0')) % 10).toString();
    },

    /**
     * Does NOT check requirements.  
     * Returns the Luhn remainder.
     * Note: 
     *   `getRemainder(value) === 0` is equivalent to `isValid(value)`. 
     *   You may want to use this method instead of `isValid` if you ensure argument 
     *   requirements on your side.
     * 
     * Requirements
     * - rawValue must be not `Null`
     * - rawValue must be of type `String`
     * 
     * @param {*} rawValue 
     */
    getRemainder: function(rawValue) {
        return getLuhnRemainder(rawValue);
    }
};

const FORMAT_ISVALID = /^[0-9]{2,}$/;
const FORMAT_GENERATE = /^[0-9]{1,}$/;

const CHARCODE_0 = '0'.charCodeAt(0);
const MAPPING_EVEN = [0, 2, 4, 6, 8, 1, 3, 5, 7, 9];

function getLuhnRemainder(value) {
    var length = value.length;
    var accumulator = 0;
    var bit = 0;

    while (length-- > 0) {
        accumulator += (bit ^= 1) ? value.charCodeAt(length) - CHARCODE_0 : MAPPING_EVEN[value.charCodeAt(length) - CHARCODE_0];
    }

    return accumulator % 10;
}

function stringifyInput(rawValue) {
    if (rawValue !== null && rawValue !== undefined) {
        if (typeof rawValue === 'string') {
            return rawValue;
            
        }
        
        throw new Error('Expecting value of type \'string\', found: \'' + (typeof rawValue) + '\'');
    }

    throw new Error('Expecting value of type \'string\', found: \'' + rawValue + '\'');
}

export default luhn;
