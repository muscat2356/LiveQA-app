import { EventStatus } from "@/types/event/event";
import { Badge } from "../ui/badge";

const MAP: Record<EventStatus, { label:string; className: string}> = {
    DRAFT: { label: "受付前", className: "bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300"},
    OPEN: { label: "受付中", className: "bg-green-100 text-green-800 dark:bg-green-500/15 dark:text-green-300"},
    CLOSED: {label: "終了", className: "bg-muted text-muted-foreground"},
};

export function StatusBadge({ status }: {status: EventStatus}) {
    const {label, className} = MAP[status];
    return <Badge variant="secondary" className={className}>{label}</Badge>
}