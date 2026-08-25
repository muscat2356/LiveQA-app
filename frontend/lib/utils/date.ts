export default function formatDateTime(dateString: string){
    return new Intl.DateTimeFormat("ja-JP",{
        timeZone: "Asia/Tokyo",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    }).format(new Date(dateString));
}