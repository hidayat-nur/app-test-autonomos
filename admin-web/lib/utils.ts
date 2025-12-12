export function extractPackageName(input: string): string {
    if (!input) return '';

    // Check if full URL
    try {
        if (input.startsWith('http') || input.startsWith('play.google.com')) {
            const urlString = input.startsWith('http') ? input : `https://${input}`;
            const url = new URL(urlString);
            const id = url.searchParams.get('id');
            if (id) return id;
        }
    } catch (e) {
        // ignore invalid URL
    }

    // Check simple regex for id=...
    const match = input.match(/[?&]id=([^&]+)/);
    if (match) return match[1];

    // If it looks like a package name (contains dot), return it as fallback
    // e.g. "com.example.app"
    if (input.includes('.') && !input.includes('/') && !input.includes(':')) {
        return input;
    }

    return '';
}
