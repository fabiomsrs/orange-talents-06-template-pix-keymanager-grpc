package br.com.zup.edu.shared

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client

@Client("\${itau.uri}")
interface ItauERPClient {
    @Get("/clientes/{clienteId}/")
    fun consultarCliente(clienteId: String): HttpResponse<ClienteItauResponse>

    @Get("/clientes/{clienteId}/contas?tipo={tipoConta}")
    fun consultarContaCliente(clienteId: String, tipoConta:String): HttpResponse<ClienteContaItauResponse>
}

data class ClienteItauResponse(val id: String, val nome: String, val cpf: String, val instituicao: InstituicaoDto)

data class ClienteContaItauResponse(
    val tipo: String,
    val instituicao: InstituicaoDto,
    val agencia: String,
    val numero: String,
    val titular: TitularDto
)

data class InstituicaoDto(val nome: String, val ispb: String)
data class TitularDto(val id: String, val nome: String, val cpf: String)