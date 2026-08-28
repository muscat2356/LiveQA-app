/**
 * document.cookieから指定の名の値を取り出す
 * ブラウザ専用
 */
export function readCookie(name: string) {
    const row = document.cookie
      .split("; ")
      .find((c) => c.startsWith(`${name}=`));
    return row ? decodeURIComponent(row.slice(name.length + 1)) : undefined;
  }