package br.com.zup.edu.chavepix

import br.com.zup.edu.ChavePixServiceGrpc
import br.com.zup.edu.ListarChavePixRequest
import br.com.zup.edu.TipoChave
import br.com.zup.edu.TipoConta
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
class ChavePixListarTest(val grpClient: ChavePixServiceGrpc.ChavePixServiceBlockingStub) {
    @Inject
    lateinit var repository: ChavePixRepository

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `teste deve retornar lista com chaves pix`(){
        repository.save(ChavePix("c56dfef4-7901-44fb-84e2-a2cefb157890", TipoChave.CPF, "12345678901", TipoConta.CACC))
        val request = ListarChavePixRequest.newBuilder().setClientId("c56dfef4-7901-44fb-84e2-a2cefb157890").build()
        val response = grpClient.listarChavePix(request)

        with(response){
            Assertions.assertEquals(chavesCount, 1)
            Assertions.assertEquals(getChaves(0).idCliente, "c56dfef4-7901-44fb-84e2-a2cefb157890")
        }
    }

    @Test
    fun `teste deve retornar lista com chaves pix vazia`(){
        val request = ListarChavePixRequest.newBuilder().setClientId("c56dfef4-7901-44fb-84e2-a2cefb157890").build()
        val response = grpClient.listarChavePix(request)

        with(response){
            Assertions.assertEquals(chavesCount, 0)
        }
    }

    @Test
    fun `teste deve retornar invalid argument cliente id deve ser informado`(){
        val request = ListarChavePixRequest.newBuilder().setClientId("").build()
        val exception = assertThrows<StatusRuntimeException> { grpClient.listarChavePix(request) }

        with(exception){
            Assertions.assertEquals(exception.status.code, Status.INVALID_ARGUMENT.code)
            Assertions.assertEquals(exception.status.description, "cliente id deve ser informado")
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