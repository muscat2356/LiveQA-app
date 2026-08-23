import { QuestionStatus } from "@/types/question/question";
import { Badge } from "../ui/badge";

const MAP: Record<QuestionStatus, { label: string; className: string }> = {
    OPEN: { label: "未回答", className: "bg-amber-100 text-amber-800 dark:bg-amber-500/15 dark:text-amber-300"},
    ANSWERED: { label: "回答済み", className: "bg-green-100 text-green-800 dark:bg-green-500/15 dark:text-green-300"},
    ARCHIVED: {label: "非表示", className: "bg-muted text-muted-foreground"},
}

export function QuestionStatusBadge({ status}: { status: QuestionStatus}) {
    const { label, className} = MAP[status];
    return <Badge variant="secondary" className={className}>{label}</Badge>
}