package br.com.zup.edu.validator

import br.com.zup.edu.ConsultarChavePixKeyManagerRequest
import br.com.zup.edu.ConsultarChavePixResponse
import br.com.zup.edu.chavepix.ChavePix
import br.com.zup.edu.chavepix.ChavePixRepository
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.*

class ChavePixConsultaKeyManagerValidator(
    val repository: ChavePixRepository,
    val responseObserver: StreamObserver<ConsultarChavePixResponse>?,
    val request: ConsultarChavePixKeyManagerRequest?
) {

    fun validate(): Boolean {
        when {
            request?.idChavePix.isNullOrBlank() -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("chave pix id deve ser informado")
                        .asRuntimeException())
                return false
            }
            repository.findById(request!!.idChavePix.toLong()).isEmpty -> {
                responseObserver?.onError(
                    Status.NOT_FOUND
                    .withDescription("chave pix id incorreto")
                    .asRuntimeException())
                return false
            }
            request!!.idCliente.isEmpty() -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                    .withDescription("id cliente deve ser informado")
                    .asRuntimeException())
                return false
            }
            repository.findById(request!!.idChavePix.toLong()).get().idClient != request!!.idCliente -> {
                responseObserver?.onError(
                    Status.PERMISSION_DENIED
                    .withDescription("chave pix nao pertence a esse cliente")
                    .asRuntimeException())
                return false
            }
        }
        return true
    }
}