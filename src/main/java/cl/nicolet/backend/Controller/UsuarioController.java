package cl.nicolet.backend.Controller;

import cl.nicolet.backend.Service.UsuarioService;
import cl.nicolet.backend.DTO.UsuarioCreateDTO;
import cl.nicolet.backend.DTO.UsuarioDTO;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//* nuevos imports */
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name="Usuarios",description = "Operaciones de gestion de usuarios")
@RestController
@RequestMapping("/api/v2/nicolet/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;


/*          Listado de Usuarios mas la busqueda por Id                  */
    @Operation(
        summary = "Listar todas los Usuarios",
        description = "Retorna la lista completa de Usuarios registradas en el sistema."
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>>getAll(){
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @Operation(summary = "Buscar Usuario por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrada"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> getById(@Parameter(description = "Id unico de la Usuario",required = true)@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }




/** crear usuario */
    @Operation(summary = "Registrar nueva Usuario")
    @ApiResponse(responseCode = "201", description = "Usuario creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Datos del nuevo producto"
            )
            @Valid @RequestBody UsuarioCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(dto));
    }






/**actualizar usuario por id */
    @Operation(summary = "Actualizar Usuario existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualización exitosa"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizar(
            @Parameter(description = "ID del Usuario a actualizar", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Nuevos datos del Usuario"
            )
            @Valid @RequestBody UsuarioCreateDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

/**Eliminar usuario por id */
    @Operation(summary = "Eliminar Usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Eliminación exitosa"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrada")
    })
    @DeleteMapping("/{id}")
      public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del Usuario a eliminar", required = true)
            @PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }



/**validacioon manual de correo existente */


    @Operation(summary ="Validacion de correo existnte")
    @ApiResponses({
        /*errores en la validacion manual del correo porblema con los code */
        @ApiResponse(responseCode ="500",description ="Correo no  existente"),
        @ApiResponse(responseCode="400",description ="Correo existente")
    })
    @PostMapping("/manual")
    public ResponseEntity<?> crearConValidacionManual(@Parameter(description = "ingrese el post para ver si correo existe")@RequestBody UsuarioCreateDTO dto){
        List<String> errores = usuarioService.validarUsuarioManual(dto);
        if(!errores.isEmpty()){
            return new ResponseEntity<>(errores, HttpStatus.BAD_REQUEST);
        }
        UsuarioDTO nuevoUsuario = usuarioService.crear(dto);
        return new ResponseEntity<>(nuevoUsuario,HttpStatus.CREATED);
    }
}