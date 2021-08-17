package br.com.zup.edu.shared.utils

import br.com.zup.edu.TipoConta

fun TipoConta?.converterParaTipoContaItau(): String {
    val map = mapOf<TipoConta, String>(Pair(TipoConta.CACC, "CONTA_CORRENTE"), Pair(TipoConta.SVGS, "CONTA_POUPANCA"))
    return map.getOrDefault(this,"CONTA_CORRENTE")
}