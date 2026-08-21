"use client"

import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Sort } from "@/types/question/question"

export default function SortTab({
    value,
    onChange
}:{
    value: Sort;
    onChange: (value: Sort) => void;
}){
    return(
        <div>
            <Tabs value={value} onValueChange={(v) => onChange(v as Sort)}>
                <TabsList>
                    <TabsTrigger value="popular">人気順</TabsTrigger>
                    <TabsTrigger value="newest">新着順</TabsTrigger>
                </TabsList>
            </Tabs>
        </div>
    );
}