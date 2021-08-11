package br.com.zup.edu.chavepix

import br.com.zup.edu.*
import br.com.zup.edu.shared.ItauERPClient
import br.com.zup.edu.validator.ChavePixRegisterValidator
import br.com.zup.edu.validator.ChavePixRemoverValidator
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChavePixServer(@Inject val chavePixRepository: ChavePixRepository,
                     @Inject val itauERPClient: ItauERPClient
): ChavePixServiceGrpc.ChavePixServiceImplBase() {

    override fun registrarChavePix(
        request: RegistrarChavePixGrpcRequest?,
        responseObserver: StreamObserver<RegistrarChavePixGrpcResponse>?
    ) {
        var chave = request!!.chave

        if (request.tipoChave == TipoChave.CHAVE_ALEATORIA) {
            chave = UUID.randomUUID().toString()
        }

        try{
            itauERPClient.consultarCliente(request!!.idCliente).let { response ->
                if(ChavePixRegisterValidator(request, responseObserver, chavePixRepository)
                    .validate() && response.status.code==200){
                    val chavePix = chavePixRepository.save(ChavePix(request.idCliente,request.tipoChave, chave, request.tipoConta))
                    responseObserver!!.onNext(RegistrarChavePixGrpcResponse
                        .newBuilder()
                        .setIdChavePix(chavePix.id.toString())
                        .build()
                    )
                    responseObserver!!.onCompleted()
                    return
                }
                responseObserver?.onError(Status.NOT_FOUND
                    .withDescription("cliente id incorreto ou não informado")
                    .asRuntimeException())

            }
        }catch (e: Exception){
            throw InternalError("algo deu errado, tente novamente mais tarde ${e}")
        }
    }

    override fun removerChavePix(
        request: RemoverChavePixGrpcRequest?,
        responseObserver: StreamObserver<RemoverChavePixGrpcResponse>?
    ) {

        if(ChavePixRemoverValidator(request, responseObserver, chavePixRepository).validate()){
            chavePixRepository.deleteById(request!!.idChavePix.toLong())
            responseObserver!!.onNext(RemoverChavePixGrpcResponse
                .newBuilder()
                .setIdChavePix(request.idChavePix)
                .build()
            )
            responseObserver!!.onCompleted()
        }
    }
}