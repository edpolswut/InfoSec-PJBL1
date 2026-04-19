package com.pucpr.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pucpr.LoginRequest.LoginRequest;
import com.pucpr.model.Usuario;
import com.pucpr.repository.UsuarioRepository;
import com.pucpr.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Classe responsável por gerenciar as requisições de Autenticação.
 */
public class AuthHandler {
    private final UsuarioRepository repository;
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthHandler(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    /**
     * Gerencia o processo de Login.
     * Objetivo: Validar credenciais e emitir um passaporte (JWT).
     */
    public void handleLogin(HttpExchange exchange) throws IOException {

        //Cors Config
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        //End Cors Config

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            return;
        }

        LoginRequest loginRequest = null;
        try {
            InputStream is = exchange.getRequestBody();

            loginRequest = mapper.readValue(is, LoginRequest.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            exchange.sendResponseHeaders(400, -1); // 400 Bad Request
            return;
        }
        Optional<Usuario> usuarioRepo = repository.findByEmail(loginRequest.getEmail());
        if(usuarioRepo.isPresent() && BCrypt.checkpw(loginRequest.getPassword(), usuarioRepo.get().getSenhaHash())){
            String token = jwtService.generateToken(usuarioRepo.get());

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("token", token);
            responseBody.put("type", "Bearer");

            byte[] responseBytes = mapper.writeValueAsBytes(responseBody);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        else{
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("status", "error");
            errorBody.put("message", "E-mail ou senha inválidos. Acesso negado.");

            byte[] errorBytes = mapper.writeValueAsBytes(errorBody);

            exchange.getResponseHeaders().set("Content-Type", "application/json");

            exchange.sendResponseHeaders(401, errorBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }
    }

    /**
     * Gerencia o processo de Cadastro (Registro).
     * Objetivo: Criar um novo usuário de forma segura.
     */
    public void handleRegister(HttpExchange exchange) throws IOException {

        //Cors Config
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        //End Cors Config

        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        JsonNode jsonNode = mapper.readTree(exchange.getRequestBody());

        String nome = jsonNode.has("name") ? jsonNode.get("name").asText() : "";
        String email = jsonNode.has("email") ? jsonNode.get("email").asText() : "";
        String senha = jsonNode.has("password") ? jsonNode.get("password").asText() : "";

         Optional<Usuario> usuarioRepo = repository.findByEmail(email);
        if(usuarioRepo.isEmpty()){

            repository.save(new Usuario(nome,
                    email,
                    BCrypt.hashpw(senha, BCrypt.gensalt(12)),
                    "Usuário"));

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("status", "success");
            successResponse.put("message", "Usuário cadastrado com sucesso!");

            byte[] responseBytes = mapper.writeValueAsBytes(successResponse);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        else{
            Map<String, String> error = new HashMap<>();
            error.put("error", "Não foi possível concluir o registro.");

            byte[] response = mapper.writeValueAsBytes(error);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, response.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}