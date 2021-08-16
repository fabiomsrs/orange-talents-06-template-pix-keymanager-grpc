package br.com.zup.edu.chavepix

import br.com.zup.edu.*
import br.com.zup.edu.shared.BCBClient
import br.com.zup.edu.shared.request.BCBPixDeleteRequest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.*
import org.mockito.BDDMockito
import org.mockito.Mockito
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ChavePixRemoverTest(
    val grpClient: ChavePixServiceGrpc.ChavePixServiceBlockingStub,
    val bcbClient: BCBClient
) {

    @Inject
    lateinit var repository: ChavePixRepository

    lateinit var chavePix: ChavePix

    @BeforeEach
    fun setUp() {
        chavePix = repository.save(ChavePix("c56dfef4-7901-44fb-84e2-a2cefb157890", TipoChave.CPF, "12345678901", TipoConta.CACC))
    }

    @AfterEach
    fun cleanUp(){
        repository.deleteAll()
    }

    @Test
    fun `deve remover uma chave pix`() {
        // Setting up mockito bcb client
        val bcbRequest = BCBPixDeleteRequest("12345678901","60701190")

        BDDMockito.`when`(bcbClient.deletarChavePix(bcbRequest.key, bcbRequest)).thenReturn(HttpResponse.ok())


        val response = grpClient.removerChavePix(
            RemoverChavePixGrpcRequest
            .newBuilder()
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setIdChavePix(chavePix.id.toString())
            .build()
        )
        with(response){
            Assertions.assertNotNull(idChavePix)
            Assertions.assertTrue(repository.findById(idChavePix.toLong()).isEmpty)
        }
    }

    @Test
    fun `deve retornar invalid argument para id cliente vazio`() {

        val request = RemoverChavePixGrpcRequest
            .newBuilder()
            .setIdCliente("")
            .setIdChavePix("32131")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.removerChavePix(request)
        }
        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "id do cliente deve ser informado")
        }
    }

    @Test
    fun `deve retornar invalid argument para chave pix vazia`() {

        val request = RemoverChavePixGrpcRequest
            .newBuilder()
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setIdChavePix("")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.removerChavePix(request)
        }
        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "id da chave pix deve ser informado")
        }
    }

    @Test
    fun `deve retornar um not found para um id inexistente`() {

        val request = RemoverChavePixGrpcRequest
            .newBuilder()
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setIdChavePix("10")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.removerChavePix(request)
        }
        with(exception){
            Assertions.assertEquals(exception.status.code, Status.NOT_FOUND.code)
            Assertions.assertEquals(exception.status.description, "id chave pix nao encontrado")
        }
    }

    @Test
    fun `deve retornar um erro de permissão para um id de chave de idClient diferente`() {

        val request = RemoverChavePixGrpcRequest
            .newBuilder()
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157891")
            .setIdChavePix(chavePix.id.toString())
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.removerChavePix(request)
        }
        with(exception){
            Assertions.assertEquals(exception.status.code, Status.PERMISSION_DENIED.code)
            Assertions.assertEquals(exception.status.description, "chave pix nao pertence a esse cliente")
        }
    }

    @MockBean(BCBClient::class)
    fun BCBClientMock(): BCBClient {
        return Mockito.mock(BCBClient::class.java)
    }
    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ChavePixServiceGrpc.ChavePixServiceBlockingStub {
            return ChavePixServiceGrpc.newBlockingStub(channel)
        }
    }
}