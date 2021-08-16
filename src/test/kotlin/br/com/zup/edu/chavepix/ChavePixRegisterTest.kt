package br.com.zup.edu.chavepix

import br.com.zup.edu.ChavePixServiceGrpc
import br.com.zup.edu.RegistrarChavePixGrpcRequest
import br.com.zup.edu.TipoChave
import br.com.zup.edu.TipoConta
import br.com.zup.edu.shared.*
import br.com.zup.edu.shared.response.BCBPixResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@MicronautTest(transactional = false)
class ChavePixRegisterTest(
    val grpClient: ChavePixServiceGrpc.ChavePixServiceBlockingStub,
    val bcbClient: BCBClient,
    val itauERPClient: ItauERPClient
) {
    @Inject
    lateinit var repository: ChavePixRepository

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `deve criar uma nova chave pix`() {
        // Setting up mockito bcb client
        val bcbRequest = BCBPixRequest("CPF",
            "12345678901",
            BankAccountDto("60701190","0001","123456",TipoConta.CACC),
            BankOwnerDto("NATURAL_PERSON", "steve jobs", "12345678901")
        )
        val bcbResponse = BCBPixResponse(bcbRequest.keyType, bcbRequest.key, bcbRequest.bankAccount, bcbRequest.owner)
        val itauResponse = ClienteContaItauResponse(TipoConta.CACC.name,
            InstituicaoDto("itau","60701190"),
            "0001",
            "123456",
            TitularDto("1", "steve jobs","12345678901")
        )

        `when`(itauERPClient.consultarContaCliente("c56dfef4-7901-44fb-84e2-a2cefb157890",TipoConta.CACC.name)).thenReturn(
            HttpResponse.ok(itauResponse))
        `when`(bcbClient.gerarChavePix(bcbRequest)).thenReturn(HttpResponse.created(bcbResponse))

        val response = grpClient.registrarChavePix(RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("12345678901")
            .setTipoChave(TipoChave.CPF)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()
        )
        with(response){
            Assertions.assertNotNull(idChavePix)
            Assertions.assertTrue(repository.findById(idChavePix.toLong()).isPresent)
        }
    }

    @Test
    fun `deve retorna invalid argument ao preencher a chave e selecionar Tipo Aleatorio`() {
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("04673696310")
            .setTipoChave(TipoChave.RANDOM)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave não deve ser preenchida no tipo chave aleatorio")
            Assertions.assertTrue(repository.findAll().isEmpty())
        }
    }

    @Test
    fun `cpf vazio deve retorna erro invalid argument`() {
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("")
            .setTipoChave(TipoChave.CPF)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave deve ser informada")
            Assertions.assertTrue(repository.findAll().isEmpty())
        }
    }

    @Test
    fun `deve criar uma chave aleatoria com UUID gerado`() {

        val bcbRequest = BCBPixRequest(TipoChave.RANDOM.name,
            "",
            BankAccountDto("60701190","0001","123456",TipoConta.CACC),
            BankOwnerDto("NATURAL_PERSON", "steve jobs", "12345678901")
        )
        val bcbResponse = BCBPixResponse(bcbRequest.keyType, UUID.randomUUID().toString(), bcbRequest.bankAccount, bcbRequest.owner)
        val itauResponse = ClienteContaItauResponse(TipoConta.CACC.name,
            InstituicaoDto("itau","60701190"),
            "0001",
            "123456",
            TitularDto("1", "steve jobs","12345678901")
        )

        `when`(itauERPClient.consultarContaCliente("c56dfef4-7901-44fb-84e2-a2cefb157890",TipoConta.CACC.name)).thenReturn(
            HttpResponse.ok(itauResponse))
        `when`(bcbClient.gerarChavePix(bcbRequest)).thenReturn(HttpResponse.created(bcbResponse))

        val response = grpClient.registrarChavePix(RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("")
            .setTipoChave(TipoChave.RANDOM)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()
        )

        with(response) {
            val chave = repository.findById(idChavePix.toLong()).get()
            Assertions.assertNotNull(idChavePix)
            Assertions.assertTrue(isUUID(chave.valor))
        }
    }

    @Test
    fun `telefone vazio deve retorna erro invalid argument`() {
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("")
            .setTipoChave(TipoChave.PHONE)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave deve ser informada")
            Assertions.assertTrue(repository.findAll().isEmpty())
        }
    }
    @Test
    fun `email vazio deve retorna erro invalid argument`() {
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("")
            .setTipoChave(TipoChave.EMAIL)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave deve ser informada")
            Assertions.assertTrue(repository.findAll().isEmpty())
        }
    }

    @Test
    fun `cpf formato invalido deve retorna erro invalid argument`() {
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("1234")
            .setTipoChave(TipoChave.CPF)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave cpf formato invalido\n" +
                    "formato esperado 12345678901")
            Assertions.assertTrue(repository.findAll().isEmpty())
        }
    }

    @Test
    fun `email formato invalido deve retorna erro invalid argument`() {
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("1234")
            .setTipoChave(TipoChave.EMAIL)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave email formato invalido\n" +
                    "formato esperado teste@teste.com")
            Assertions.assertTrue(repository.findAll().isEmpty())
        }
    }

    @Test
    fun `telefone formato invalido deve retorna erro invalid argument`() {
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("1234")
            .setTipoChave(TipoChave.PHONE)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave telefone formato invalido\n" +
                    "formato esperado +5585988714077")
            Assertions.assertTrue(repository.findAll().isEmpty())
        }
    }

    @Test
    fun `retornar already exists em caso de chave repedita`() {
        repository.save(ChavePix("c56dfef4-7901-44fb-84e2-a2cefb157890", TipoChave.CPF, "12345678901", TipoConta.CACC))
        val request = RegistrarChavePixGrpcRequest
            .newBuilder()
            .setChave("12345678901")
            .setTipoChave(TipoChave.CPF)
            .setTipoConta(TipoConta.CACC)
            .setIdCliente("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.registrarChavePix(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.ALREADY_EXISTS.code)
            Assertions.assertEquals(exception.status.description, "chave ja existe")
        }
    }

    @MockBean(BCBClient::class)
    fun BCBClientMock(): BCBClient {
        return Mockito.mock(BCBClient::class.java)
    }

    @MockBean(ItauERPClient::class)
    fun ItauERPClientMock(): ItauERPClient {
        return Mockito.mock(ItauERPClient::class.java)
    }

    fun isUUID(string: String?): Boolean {
        return try {
            UUID.fromString(string)
            true
        } catch (ex: Exception) {
            false
        }
    }
    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ChavePixServiceGrpc.ChavePixServiceBlockingStub {
            return ChavePixServiceGrpc.newBlockingStub(channel)
        }
    }
}