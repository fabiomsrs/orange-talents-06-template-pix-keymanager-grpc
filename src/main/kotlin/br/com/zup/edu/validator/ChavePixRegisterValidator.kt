package br.com.zup.edu.validator

import br.com.zup.edu.RegistrarChavePixGrpcRequest
import br.com.zup.edu.RegistrarChavePixGrpcResponse
import br.com.zup.edu.TipoChave
import br.com.zup.edu.chavepix.ChavePixRepository
import io.grpc.Status
import io.grpc.stub.StreamObserver

class ChavePixRegisterValidator(
    val request: RegistrarChavePixGrpcRequest?,
    val responseObserver: StreamObserver<RegistrarChavePixGrpcResponse>?,
    val chavePixRepository: ChavePixRepository
) {

    fun validate(): Boolean {
        when {
            request!!.tipoChave == TipoChave.RANDOM && !request.chave.isNullOrEmpty()-> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                        .withDescription("chave não deve ser preenchida no tipo chave aleatorio")
                        .asRuntimeException())
                return false
            }
            request!!.chave.isNullOrBlank() && request.tipoChave != TipoChave.RANDOM -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                    .withDescription("chave deve ser informada")
                    .asRuntimeException())
                return false
            }
            chavePixRepository.findByValor(request!!.chave).isNotEmpty() -> {
                responseObserver?.onError(
                    Status.ALREADY_EXISTS
                    .withDescription("chave ja existe")
                    .asRuntimeException())
                return false
            }
            !request.chave.matches("^[0-9]{11}\$".toRegex()) && request.tipoChave == TipoChave.CPF -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                    .withDescription("chave cpf formato invalido")
                    .augmentDescription("formato esperado 12345678901")
                    .asRuntimeException())
                return false
            }
            !request.chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex()) && request.tipoChave == TipoChave.PHONE -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                    .withDescription("chave telefone formato invalido")
                    .augmentDescription("formato esperado +5585988714077")
                    .asRuntimeException())
                return false
            }
            !request.chave.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex()) && request.tipoChave == TipoChave.EMAIL -> {
                responseObserver?.onError(
                    Status.INVALID_ARGUMENT
                    .withDescription("chave email formato invalido")
                    .augmentDescription("formato esperado teste@teste.com")
                    .asRuntimeException())
                return false
            }
        }
        return true
    }
}