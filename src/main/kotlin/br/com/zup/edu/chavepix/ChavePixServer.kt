package br.com.zup.edu.chavepix

import br.com.zup.edu.*
import br.com.zup.edu.shared.*
import br.com.zup.edu.shared.request.BCBPixDeleteRequest
import br.com.zup.edu.validator.ChavePixRegisterValidator
import br.com.zup.edu.validator.ChavePixRemoverValidator
import io.grpc.Status
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChavePixServer(@Inject val chavePixRepository: ChavePixRepository,
                     @Inject val itauERPClient: ItauERPClient,
                     @Inject val bcbClient: BCBClient
): ChavePixServiceGrpc.ChavePixServiceImplBase() {

    override fun registrarChavePix(
        request: RegistrarChavePixGrpcRequest?,
        responseObserver: StreamObserver<RegistrarChavePixGrpcResponse>?
    ) {

        try{
            itauERPClient.consultarContaCliente(request!!.idCliente, request!!.tipoConta.name).let { response ->
                if(ChavePixRegisterValidator(request, responseObserver, chavePixRepository)
                    .validate() && response.status.code==200){
                    val body = response.body()
                    val bcbPixRequest = BCBPixRequest(request!!.tipoChave.name,
                        request!!.chave,
                        BankAccountDto(body.instituicao.ispb,body.agencia,body.numero,"CACC"),
                        BankOwnerDto("NATURAL_PERSON",body.titular.nome, body.titular.cpf)
                    )
                    bcbClient.gerarChavePix(bcbPixRequest).let { response ->
                        val chavePix = chavePixRepository.save(
                            ChavePix(
                                request.idCliente,
                                request.tipoChave,
                                response.body().key,
                                request.tipoConta,
                            )
                        )
                        responseObserver!!.onNext(RegistrarChavePixGrpcResponse
                            .newBuilder()
                            .setIdChavePix(chavePix.id.toString())
                            .build()
                        )
                        responseObserver!!.onCompleted()
                    }
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
            val itauResponse = itauERPClient.consultarCliente(request!!.idCliente)
                .body()

            val optionalChavePix = chavePixRepository.findById(request!!.idChavePix.toLong())

            if(optionalChavePix.isEmpty){
                responseObserver?.onError(Status.NOT_FOUND
                    .withDescription("chave pix id incorreto ou não informado")
                    .asRuntimeException())
                return
            }

            val chavePix = optionalChavePix.get()

            bcbClient.deletarChavePix(
                chavePix.valor,
                BCBPixDeleteRequest(chavePix.valor, itauResponse.instituicao.ispb),
            )
                .takeIf { it.status.code == 200 }
                ?.run {
                    chavePixRepository.delete(chavePix)
                    responseObserver!!.onNext(RemoverChavePixGrpcResponse
                        .newBuilder()
                        .setIdChavePix(request.idChavePix)
                        .build()
                    )
                    responseObserver!!.onCompleted()
                }
        }
    }
}