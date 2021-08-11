package br.com.zup.edu.chavepix

import br.com.zup.edu.*
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class ChavePixRemoverTest(
    val grpClient: ChavePixServiceGrpc.ChavePixServiceBlockingStub
) {

    @Inject
    lateinit var repository: ChavePixRepository

    lateinit var chavePix: ChavePix

    @BeforeEach
    fun setUp() {
        chavePix = repository.save(ChavePix("c56dfef4-7901-44fb-84e2-a2cefb157890", TipoChave.CPF, "12345678901", TipoConta.CORRENTE))
    }

    @AfterEach
    fun cleanUp(){
        repository.deleteAll()
    }

    @Test
    fun `deve remover uma chave pix`() {

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

}