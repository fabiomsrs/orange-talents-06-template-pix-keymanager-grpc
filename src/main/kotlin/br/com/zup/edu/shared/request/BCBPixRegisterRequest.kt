package br.com.zup.edu.shared

import br.com.zup.edu.TipoConta


class BCBPixRequest(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccountDto,
    val owner: BankOwnerDto
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BCBPixRequest

        if (owner != other.owner) return false

        return true
    }
    override fun hashCode(): Int {
        return key.hashCode()
    }
}

data class BankAccountDto(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: TipoConta
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BankAccountDto
        if (accountNumber == other.accountNumber && accountType == other.accountType){
            return true
        }
        return true
    }
    override fun hashCode(): Int {
        return accountNumber.hashCode()
    }
}

data class BankOwnerDto(
    val type: String,
    val name: String,
    val taxIdNumber: String
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BankOwnerDto
        if (taxIdNumber == other.taxIdNumber){
            return true
        }
        return true
    }
    override fun hashCode(): Int {
        return taxIdNumber.hashCode()
    }
}
