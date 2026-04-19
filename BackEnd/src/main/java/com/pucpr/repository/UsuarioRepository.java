package com.pucpr.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.model.Usuario;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {
    private final String FILE_PATH = "usuarios.json";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Busca um usuário pelo e-mail dentro do arquivo JSON.
     */
    public Optional<Usuario> findByEmail(String email) {
        List<Usuario> Usuarios = findAll();

        List<Usuario> UsuariosFiltrado = Usuarios.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email)).toList();

        if(!UsuariosFiltrado.isEmpty()){
            return Optional.of(UsuariosFiltrado.getFirst());
        }
        return Optional.empty();
    }

    /**
     * Retorna todos os usuários cadastrados no arquivo JSON.
     */
    public List<Usuario> findAll() {
        try{
            List<Usuario> Usuarios = mapper.readValue(
                    new File(FILE_PATH),
                    new TypeReference<List<Usuario>>(){}
            );
            return Usuarios;
        }
        catch (Exception ex){
            return new ArrayList<>();
        }
    }

    /**
     * Salva um novo usuário no arquivo JSON.
     */
    public void save(Usuario usuario) throws IOException {
        if (findByEmail(usuario.getEmail()).isEmpty()) {
            List<Usuario> Usuarios = findAll();
            Usuarios.add(usuario);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), Usuarios);
        }
    }
}