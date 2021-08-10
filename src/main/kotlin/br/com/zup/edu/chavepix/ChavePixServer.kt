package br.com.zup.edu.chavepix

import br.com.zup.edu.*
import br.com.zup.edu.shared.ItauERPClient
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.util.*
import javax.inject.Singleton

@Singleton
class ChavePixServer(val chavePixRepository: ChavePixRepository,
    val itauERPClient: ItauERPClient
): ChavePixServiceGrpc.ChavePixServiceImplBase() {

    override fun registrarChavePix(
        request: RegistrarChavePixGrpcRequest?,
        responseObserver: StreamObserver<RegistrarChavePixGrpcResponse>?
    ) {
        var chave = request!!.chave
        when {
            request!!.tipoConta.toString().isNullOrBlank() -> {
                val error = Status.INVALID_ARGUMENT
                    .withDescription("tipo conta deve ser informada")
                    .asRuntimeException()
                responseObserver?.onError(error)
                return
            }
            request!!.chave.isNullOrBlank() && request.tipoChave != TipoChave.CHAVE_ALEATORIA -> {
                val error = Status.INVALID_ARGUMENT
                    .withDescription("chave deve ser informada")
                    .asRuntimeException()
                responseObserver?.onError(error)
                return
            }
            chavePixRepository.existsByChave(request!!.chave) -> {
                val error = Status.ALREADY_EXISTS
                    .withDescription("chave ja existe")
                    .asRuntimeException()
                responseObserver?.onError(error)
                return
            }
            !request.chave.matches("^[0-9]{11}\$".toRegex()) && request.tipoChave == TipoChave.CEP -> {
                val error = Status.INVALID_ARGUMENT
                    .withDescription("chave cep formato invalida")
                    .augmentDescription("formato esperado 12345678901")
                    .asRuntimeException()
                responseObserver?.onError(error)
                return
            }
            !request.chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex()) && request.tipoChave == TipoChave.TELEFONE -> {
                val error = Status.INVALID_ARGUMENT
                    .withDescription("chave telefone formato invalida")
                    .augmentDescription("formato esperado +5585988714077")
                    .asRuntimeException()
                responseObserver?.onError(error)
                return
            }
            !request.chave.matches("^[A-Za-z0-9+_.-]+@(.+)\\\$".toRegex()) && request.tipoChave == TipoChave.EMAIL -> {
                val error = Status.INVALID_ARGUMENT
                    .withDescription("chave email formato invalida")
                    .augmentDescription("formato esperado teste@teste.com")
                    .asRuntimeException()
                responseObserver?.onError(error)
                return
            }
            request.chave.isNullOrBlank() && request.tipoChave == TipoChave.CHAVE_ALEATORIA -> {
                chave = UUID.randomUUID().toString()
            }

        }

        try{
            itauERPClient.consultarCliente(request!!.idCliente).let { response ->
                if(response.status.code == 200){
                    val chavePix = chavePixRepository.save(ChavePix(request.tipoChave, chave, request.tipoConta))
                    responseObserver!!.onNext(RegistrarChavePixGrpcResponse
                        .newBuilder()
                        .setIdChavePix(chavePix.id.toString())
                        .build()
                    )
                    responseObserver!!.onCompleted()
                }
            }
        }catch (e: StatusRuntimeException){
            println("deu ruim ${e.status.code}")
            responseObserver!!.onCompleted()
        }
    }
}