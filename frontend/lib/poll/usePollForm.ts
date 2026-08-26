"use client"

import { PollType } from "@/types/poll/poll";
import { useState } from "react";

//選択肢に活用
export type OptionInput = {
    key: number;
    label: string;
}

//採番設定 再レンダリングで初期化を防ぐため
let count = 0;
const newOption = ():OptionInput => ({key: count++, label:""});

export default function PollForm(){

    const [ questionText, setQuestionText ] = useState("");
    const [ pollType, setPollType ] = useState<PollType>("SINGLE");
    const [ options, setOptions ] = useState<OptionInput[]>([newOption(), newOption()]);


    //追加関数
    const addOption = () => setOptions((prev) => [...prev, newOption()]);

    //削除関数
    const removeOption = (key: number) =>
            setOptions((prev) => prev.filter((o) => o.key !== key));

    //編集関数
    const editOption = (key: number, label: string) => 
            setOptions((prev) => prev.map((o) => (o.key === key ? { ...o, label} : o)));

    //リセット関数　初期値にすべて戻す
    const reset = () =>{
        setQuestionText("");
        setPollType("SINGLE");
        setOptions([newOption(), newOption()]);
    }

    const trimmedText = questionText.trim();
    const labels = options.map((o) => o.label.trim()).filter((l) => l !== "");
    const hasDuplicate = new Set(labels).size !== labels.length;

    const invalid = 
        trimmedText.length < 1 || trimmedText.length > 200 ||
        labels.length < 2 || labels.length > 10 ||
        labels.some((l) => l.length > 100) ||
        hasDuplicate;

    return {
        questionText, setQuestionText,
        pollType, setPollType,
        options, addOption, removeOption, editOption,
        reset,
        trimmedText, labels, hasDuplicate, invalid,
    };
    
}

