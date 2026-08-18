import { EventStatus } from "@/types/event/event";
import { Badge } from "../ui/badge";

const MAP: Record<EventStatus, { label:string; className: string}> = {
    DRAFT: { label: "受付前", className: "bg-muted text-muted-foreground"},
    OPEN: { label: "受付中", className: "bg-green-100 text-green-900"},
    CLOSED: {label: "終了", className: "bg-muted text-muted-foreground"},
};

export function StatusBadge({ status }: {status: EventStatus}) {
    const {label, className} = MAP[status];
    return <Badge variant="secondary" className={className}>{label}</Badge>
}