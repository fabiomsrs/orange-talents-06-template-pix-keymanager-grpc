package br.com.zup.edu.validator

import br.com.zup.edu.ListarChavePixRequest
import br.com.zup.edu.ListarChavePixResponse
import io.grpc.Status
import io.grpc.stub.StreamObserver

class ChavePixListarValidator(
    val request: ListarChavePixRequest?,
    val responseObserver: StreamObserver<ListarChavePixResponse>?
) {
    fun validate(): Boolean {
        if(request!!.clientId.isNullOrEmpty()){
            responseObserver?.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("cliente id deve ser informado")
                    .asRuntimeException())
            return false
        }
        return true
    }
}