/**
 * Latin → Aksara Jawa (Hanacaraka / "honocoroko") phonetic transliterator.
 *
 * Scope & limitations (IMPORTANT — please have a Javanese reader verify):
 * - Transliterates natural-language Latin text phonetically. It is NOT a
 *   translation — Indonesian words keep their spelling, only the script changes.
 * - Closed syllables / consonant clusters use *pangkon* (virama) rather than
 *   traditional *pasangan* stacking. This is a legitimate, readable simplification
 *   but not the most traditional form.
 * - Indonesian "e" is ambiguous (pepet vs. taling); we default to *pepet* (ꦼ),
 *   which is correct for most Indonesian words but may be wrong for some names.
 * - Digits, currency ("Rp"), punctuation, whitespace and anything that is not a
 *   run of Latin letters are passed through unchanged — so nominal amounts,
 *   invoice codes and domains stay in Latin as requested.
 */

// Base consonants — each carries the inherent vowel "a".
const CONS: Record<string, string> = {
    h: 'ꦲ', n: 'ꦤ', c: 'ꦕ', r: 'ꦫ', k: 'ꦏ', d: 'ꦢ', t: 'ꦠ', s: 'ꦱ', w: 'ꦮ',
    l: 'ꦭ', p: 'ꦥ', j: 'ꦗ', y: 'ꦪ', m: 'ꦩ', g: 'ꦒ', b: 'ꦧ',
    // digraphs
    ng: 'ꦔ', ny: 'ꦚ', dh: 'ꦣ', th: 'ꦛ',
    // foreign sounds (cecak telu ꦳ marks a borrowed consonant)
    f: 'ꦥ꦳', v: 'ꦥ꦳', z: 'ꦗ꦳', kh: 'ꦏ꦳', sy: 'ꦱ꦳', q: 'ꦏ',
};

// Vowel signs (sandhangan swara), applied after a base consonant.
const SWARA: Record<string, string> = {
    a: '', i: 'ꦶ', u: 'ꦸ', e: 'ꦼ', o: 'ꦺꦴ',
};

const VOWELS = new Set(['a', 'i', 'u', 'e', 'o']);
const DIGRAPHS = ['ng', 'ny', 'dh', 'th', 'kh', 'sy', 'ch'];

const PANGKON = '꧀'; // virama — kills the inherent vowel of a coda consonant
const CECAK = 'ꦁ';   // final -ng
const LAYAR = 'ꦂ';   // final -r
const WIGNYAN = 'ꦃ'; // final -h
const AKARA = 'ꦲ';   // "ha" used as a bare-vowel carrier (word-initial vowels)

type Token = { type: 'c' | 'v'; val: string };

function tokenize(word: string): Token[] {
    const w = word.toLowerCase().replace(/x/g, 'ks'); // x → ks
    const tokens: Token[] = [];
    let i = 0;
    while (i < w.length) {
        const two = w.slice(i, i + 2);
        if (DIGRAPHS.includes(two)) {
            tokens.push({ type: 'c', val: two === 'ch' ? 'c' : two });
            i += 2;
            continue;
        }
        const ch = w[i];
        if (VOWELS.has(ch)) tokens.push({ type: 'v', val: ch });
        else tokens.push({ type: 'c', val: ch });
        i += 1;
    }
    return tokens;
}

function transliterateWord(word: string): string {
    const tokens = tokenize(word);
    let out = '';
    let i = 0;
    while (i < tokens.length) {
        const tk = tokens[i];
        if (tk.type === 'v') {
            // Bare vowel (word-initial or hiatus) → ha-carrier + vowel sign.
            out += AKARA + SWARA[tk.val];
            i += 1;
            continue;
        }
        const base = CONS[tk.val];
        if (base === undefined) {
            // Unknown letter — skip so we never emit a broken carrier.
            i += 1;
            continue;
        }
        const next = tokens[i + 1];
        if (next && next.type === 'v') {
            out += base + SWARA[next.val];
            i += 2;
        } else {
            // Coda consonant (no following vowel).
            if (tk.val === 'ng') out += CECAK;
            else if (tk.val === 'r') out += LAYAR;
            else if (tk.val === 'h') out += WIGNYAN;
            else out += base + PANGKON;
            i += 1;
        }
    }
    return out;
}

/**
 * Transliterate a string to Aksara Jawa. Only runs of Latin letters are
 * converted; digits, punctuation and whitespace are left untouched.
 */
export function toAksara(text: string): string {
    if (!text) return text;
    return text.replace(/[A-Za-z]+/g, (m) => transliterateWord(m));
}
