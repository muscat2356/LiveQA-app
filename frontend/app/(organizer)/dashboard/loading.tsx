import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
    return (
        <div className="app-container py-8 sm:py-12">
            <Skeleton className="mb-8 h-9 w-56" />
            <div className="grid gap-4 sm:grid-cols-2">
                {Array.from({ length: 3 }).map((_, i) => (
                    <Card key={i}>
                        <CardContent className="space-y-2.5">
                            <Skeleton className="h-5 w-64" />
                            <Skeleton className="h-4 w-32" />
                            <Skeleton className="h-4 w-40" />
                        </CardContent>
                    </Card>
                ))}
            </div>
        </div>
    );
}