package br.com.zup.edu.validator

import br.com.zup.edu.RemoverChavePixGrpcRequest
import br.com.zup.edu.RemoverChavePixGrpcResponse
import br.com.zup.edu.chavepix.ChavePixRepository
import io.grpc.Status
import io.grpc.stub.StreamObserver

class ChavePixRemoverValidator(
    val request: RemoverChavePixGrpcRequest?,
    val responseObserver: StreamObserver<RemoverChavePixGrpcResponse>?,
    val chavePixRepository: ChavePixRepository
) {
    fun validate(): Boolean {
        when {
            request!!.idChavePix.isNullOrEmpty() -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("id da chave pix deve ser informado")
                        .asRuntimeException())
                return false
            }
            request!!.idCliente.isNullOrEmpty() -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("id do cliente deve ser informado")
                        .asRuntimeException())
                return false
            }
            chavePixRepository.findById(request!!.idChavePix.toLong()).isEmpty -> {
                responseObserver?.onError(
                    Status.NOT_FOUND
                        .withDescription("id chave pix nao encontrado")
                        .asRuntimeException())
                return false
            }
            chavePixRepository.findById(request!!.idChavePix.toLong()).get().idClient != request!!.idCliente -> {
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