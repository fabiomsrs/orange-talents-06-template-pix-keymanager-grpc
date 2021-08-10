package br.com.zup.edu.chavepix

import br.com.zup.edu.TipoChave
import br.com.zup.edu.TipoConta
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class ChavePix(
    val tipoChave: TipoChave,
    val valor: String,
    val tipoConta: TipoConta
){
    @Id
    @GeneratedValue
    var id: Long? = null
}