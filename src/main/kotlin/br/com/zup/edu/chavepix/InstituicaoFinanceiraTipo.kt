package br.com.zup.edu.chavepix

enum class InstituicaoFinanceiraTipo() {
    ITAU,
    OUTROBANCO;

    companion object{
        fun converter(participant: String): InstituicaoFinanceiraTipo {
            val map = mapOf<String, InstituicaoFinanceiraTipo>(Pair("60701190", ITAU))
            return map.getOrDefault(participant, OUTROBANCO)
        }
    }
}