package br.com.zup.edu.chavepix

import br.com.zup.edu.*
import br.com.zup.edu.shared.BCBClient
import br.com.zup.edu.shared.BankAccountDto
import br.com.zup.edu.shared.BankOwnerDto
import br.com.zup.edu.shared.response.BCBPixConsultaResponse
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
class ChavePixConsultaTest(val grpClient: ChavePixServiceGrpc.ChavePixServiceBlockingStub,
                           val bcbClient: BCBClient) {

    @Inject
    lateinit var repository: ChavePixRepository

    @Test
    fun `teste consulta KEY MANAGER deve retornar os dados esperados`(){
        val chave = repository.save(ChavePix("123456",TipoChave.CPF, "12345678901",TipoConta.CACC))
        mock(chave)

        val response = grpClient.consultarChavePixKeyManager(ConsultarChavePixKeyManagerRequest
            .newBuilder()
            .setIdChavePix(chave.id.toString())
            .setIdCliente(chave.idClient).build()
        )
        with(response){
            Assertions.assertNotNull(response.idCliente)
            Assertions.assertNotNull(response.idChavePix)
            Assertions.assertNotNull(response.chave)
            Assertions.assertEquals(response.chave, chave.valor)
            Assertions.assertEquals(response.idCliente, chave.idClient)
            Assertions.assertEquals(response.idChavePix, chave.id.toString())

        }
    }

    @Test
    fun `teste consulta Microsservico deve retornar os dados esperados`(){

        val chave = repository.save(ChavePix("123456",TipoChave.CPF, "12345678901",TipoConta.CACC))
        mock(chave)

        val response = grpClient.consultarChavePixMicrosservicos(ConsultarChavePixMicrosservicosRequest
            .newBuilder()
            .setChave(chave.valor)
            .build()
        )
        with(response){
            Assertions.assertNotNull(response.chave)
            Assertions.assertEquals(response.chave, chave.valor)
        }
    }

    @Test
    fun `teste consulta Key Manager deve retornar erro invalid argument id chave pix deve ser enviado`(){

        val chave = repository.save(ChavePix("123456",TipoChave.CPF, "12345678901",TipoConta.CACC))

        val request = ConsultarChavePixKeyManagerRequest
            .newBuilder()
            .setIdChavePix("")
            .setIdCliente(chave.idClient).build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.consultarChavePixKeyManager(request)
        }
        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave pix id deve ser informado")
        }
    }

    @Test
    fun `teste consulta Key Manager deve retornar erro invalid argument id chave incorreto`(){

        val chave = repository.save(ChavePix("123456",TipoChave.CPF, "12345678901",TipoConta.CACC))

        val request = ConsultarChavePixKeyManagerRequest
            .newBuilder()
            .setIdChavePix("12332131")
            .setIdCliente(chave.idClient).build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.consultarChavePixKeyManager(request)
        }
        with(exception){
            Assertions.assertEquals(exception.status.code, Status.NOT_FOUND.code)
            Assertions.assertEquals(exception.status.description, "chave pix id incorreto")
        }
    }

    @Test
    fun `teste consulta Key Manager deve retornar erro invalid argument client id deve ser informado`(){
        val chave = repository.save(ChavePix("123456",TipoChave.CPF, "12345678901",TipoConta.CACC))

        val request = ConsultarChavePixKeyManagerRequest
            .newBuilder()
            .setIdChavePix(chave.id.toString())
            .setIdCliente("").build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.consultarChavePixKeyManager(request)
        }
        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "id cliente deve ser informado")
        }
    }

    @Test
    fun `teste consulta Microsservicos deve retornar erro invalid argument chave deve ser informado`(){
        val request = ConsultarChavePixMicrosservicosRequest
            .newBuilder()
            .setChave("")
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.consultarChavePixMicrosservicos(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave pix deve ser informada")
        }
    }

    @Test
    fun `teste consulta Microsservicos deve retornar erro invalid argument chave muito grande`(){
        val request = ConsultarChavePixMicrosservicosRequest
            .newBuilder()
            .setChave("a".toString().repeat(78))
            .build()

        val exception = assertThrows<StatusRuntimeException> {
            grpClient.consultarChavePixMicrosservicos(request)
        }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "chave pix maior que 77 caractere")
        }
    }

    fun mock(chave: ChavePix) {
        val mockitoResponse = BCBPixConsultaResponse(
            TipoChave.CPF,
            "12345678901",
            BankAccountDto(
                "123321",
                "0001",
                "1234",
                TipoConta.CACC
            ),
            BankOwnerDto(
                "NATURAL_PERSON",
                "fulano",
                "12345678901"
            ),
            LocalDateTime.now()
        )
        `when`(bcbClient.consultarChavePix(chave.valor)).thenReturn(HttpResponse.ok(mockitoResponse))
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