
function readCookie(name: string) {
    const row = document.cookie.split("; ").find((c) => c.startsWith(`${name}=`));
    return row ? decodeURIComponent(row.slice(name.length + 1)) : undefined;
}