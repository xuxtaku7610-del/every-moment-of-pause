package com.emop.pause.engine.pet

/** 이름 뒤 조사 (받침 유무). 한글이 아니면 받침 없음으로 본다. 예: 콩이는 · 밤은, 콩이가 · 밤이, 콩이와 · 밤과. */
object Josa {
    private fun hasBatchim(word: String): Boolean {
        val c = word.trimEnd().lastOrNull() ?: return false
        if (c !in '가'..'힣') return false
        return (c - '가') % 28 != 0
    }

    fun eunNeun(name: String) = name + if (hasBatchim(name)) "은" else "는"
    fun iGa(name: String) = name + if (hasBatchim(name)) "이" else "가"
    fun waGwa(name: String) = name + if (hasBatchim(name)) "과" else "와"
}
