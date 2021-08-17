package br.com.zup.edu.chavepix

import br.com.zup.edu.*
import br.com.zup.edu.shared.*
import br.com.zup.edu.shared.request.BCBPixDeleteRequest
import br.com.zup.edu.shared.utils.converterParaTipoContaItau
import br.com.zup.edu.validator.*
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpStatus
import java.time.ZoneOffset
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
            itauERPClient.consultarContaCliente(request!!.idCliente, request!!.tipoConta.converterParaTipoContaItau()).let { response ->
                if(ChavePixRegisterValidator(request, responseObserver, chavePixRepository)
                    .validate() && response.status.code==200){
                    val body = response.body()
                    val bcbPixRequest = BCBPixRequest(request!!.tipoChave.name,
                        request!!.chave,
                        BankAccountDto(body.instituicao.ispb,body.agencia,body.numero,TipoConta.CACC),
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

    override fun consultarChavePixKeyManager(
        request: ConsultarChavePixKeyManagerRequest?,
        responseObserver: StreamObserver<ConsultarChavePixResponse>?
    ) {
        if (ChavePixConsultaKeyManagerValidator(chavePixRepository, responseObserver, request).validate()){
            val chavePix = chavePixRepository.findById(request!!.idChavePix.toLong()).get()
            try {
                bcbClient.consultarChavePix(chavePix.valor).let { response ->
                    if(response.status.code == HttpStatus.NOT_FOUND.code) {
                        responseObserver?.onError(Status.NOT_FOUND
                            .withDescription("cliente não encontrada no registro do BCB")
                            .asRuntimeException())
                        return
                    }
                    responseObserver!!.onNext(
                        ConsultarChavePixResponse
                            .newBuilder()
                            .setChave(chavePix.valor)
                            .setTipoChave(chavePix.tipoChave)
                            .setIdChavePix(chavePix.id.toString())
                            .setIdCliente(request!!.idCliente)
                            .setCpf(response!!.body().owner.taxIdNumber)
                            .setNome(response!!.body().owner.name)
                            .setConta(Conta.newBuilder()
                                .setTipoConta(chavePix.tipoConta)
                                .setAgencia(response!!.body().bankAccount.branch)
                                .setInstituicao(InstituicaoFinanceiraTipo.converter(response!!.body().bankAccount.participant).name)
                                .setNumero(response!!.body().bankAccount.accountNumber)
                                .build()
                            )
                            .setCriadoEm(Timestamp
                                    .newBuilder()
                                    .setSeconds(response.body().createdAt.toInstant(ZoneOffset.UTC).epochSecond)
                                    .setNanos(response.body().createdAt.toInstant(ZoneOffset.UTC).nano)
                                    .build()
                            )
                            .build()
                    )
                    responseObserver.onCompleted()
                }
            }catch (e: Exception){
                throw InternalError("algo deu errado, tente novamente mais tarde ${e}")
            }
        }
        return
    }

    override fun consultarChavePixMicrosservicos(
        request: ConsultarChavePixMicrosservicosRequest?,
        responseObserver: StreamObserver<ConsultarChavePixResponse>?
    ) {
        if (ChavePixConsultaMicrosservicoValidator(responseObserver, request).validate()){
            try {
                bcbClient.consultarChavePix(request!!.chave).let { response ->
                    if(response.status.code == HttpStatus.NOT_FOUND.code) {
                        responseObserver?.onError(Status.NOT_FOUND
                            .withDescription("chave não encontrada nos registros do BCB")
                            .asRuntimeException())
                        return
                    }
                    responseObserver!!.onNext(
                        ConsultarChavePixResponse
                            .newBuilder()
                            .setChave(response!!.body().key)
                            .setTipoChave(response!!.body().keyType)
                            .setCpf(response!!.body().owner.taxIdNumber)
                            .setNome(response!!.body().owner.name)
                            .setConta(Conta.newBuilder()
                                .setTipoConta(response.body().bankAccount.accountType)
                                .setAgencia(response!!.body().bankAccount.branch)
                                .setInstituicao(InstituicaoFinanceiraTipo.converter(response!!.body().bankAccount.participant).name)
                                .setNumero(response!!.body().bankAccount.accountNumber)
                                .build()
                            )
                            .setCriadoEm(Timestamp
                                .newBuilder()
                                .setSeconds(response.body().createdAt.toInstant(ZoneOffset.UTC).epochSecond)
                                .setNanos(response.body().createdAt.toInstant(ZoneOffset.UTC).nano)
                                .build()
                            )
                            .build()
                    )
                    responseObserver.onCompleted()
                }
            }catch (e: Exception){
                throw InternalError("algo deu errado, tente novamente mais tarde ${e}")
            }
        }
        return
    }

    override fun listarChavePix(
        request: ListarChavePixRequest?,
        responseObserver: StreamObserver<ListarChavePixResponse>?
    ) {
        if(ChavePixListarValidator(request, responseObserver).validate()){
            val chaves = chavePixRepository.findByIdClient(request!!.clientId).map {
                ListarChavePixResponse.Chave
                    .newBuilder()
                    .setIdCliente(it.idClient)
                    .setIdChavePix(it.id.toString())
                    .setChave(it.valor)
                    .setTipoChave(it.tipoChave)
                    .setTipoConta(it.tipoConta)
                    .setCriadoEm(Timestamp
                        .newBuilder()
                        .setSeconds(it.createdAt.toEpochSecond(ZoneOffset.UTC))
                        .setNanos(it.createdAt.nano)
                        .build()
                    )
                    .build()
            }
            responseObserver!!.onNext(ListarChavePixResponse
                .newBuilder()
                .addAllChaves(chaves)
                .build()
            )
            responseObserver!!.onCompleted()
        }
    }

}