package br.com.zup.edu.validator

import br.com.zup.edu.ConsultarChavePixMicrosservicosRequest
import br.com.zup.edu.ConsultarChavePixResponse
import br.com.zup.edu.chavepix.ChavePixRepository
import io.grpc.Status
import io.grpc.stub.StreamObserver

class ChavePixConsultaMicrosservicoValidator(
    val responseObserver: StreamObserver<ConsultarChavePixResponse>?,
    val request: ConsultarChavePixMicrosservicosRequest?,
) {
    fun validate(): Boolean {
        when {
            request!!.chave.length > 77 -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("chave pix maior que 77 caractere")
                        .asRuntimeException())
                return false
            }
            request!!.chave.isNullOrBlank() -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("chave pix deve ser informada")
                        .asRuntimeException())
                return false
            }
        }
        return true
    }

}
