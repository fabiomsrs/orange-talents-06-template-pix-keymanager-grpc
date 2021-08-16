package br.com.zup.edu.chavepix

import br.com.zup.edu.TipoChave
import br.com.zup.edu.TipoConta
import org.hibernate.annotations.CreationTimestamp
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class ChavePix(
    val idClient: String,
    val tipoChave: TipoChave,
    val valor: String,
    val tipoConta: TipoConta,
){
    @Id
    @GeneratedValue
    var id: Long? = null

    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
}