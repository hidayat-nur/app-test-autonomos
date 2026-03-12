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
    } catch {
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

export function getTodayDate(): string {
    const d = new Date();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

export function addDays(dateStr: string, days: number): string {
    const [year, month, day] = dateStr.split('-').map(Number);
    const d = new Date(year, month - 1, day);
    d.setDate(d.getDate() + days);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const d_ = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${d_}`;
}

export function getLocalDateFromTimestamp(timestamp: number): string {
    const d = new Date(timestamp);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}
