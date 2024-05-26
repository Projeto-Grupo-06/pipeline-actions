package com.bitequest.BiteQuest.service;

import com.bitequest.BiteQuest.dto.UsuarioLoginDto;
import com.bitequest.BiteQuest.dto.UsuarioTokenDto;
import com.bitequest.BiteQuest.entity.Comentario;
import com.bitequest.BiteQuest.entity.Restaurante;
import com.bitequest.BiteQuest.entity.Usuario;
import com.bitequest.BiteQuest.entity.exception.UsuarioNaoEncontradoException;
import com.bitequest.BiteQuest.jwt.GerenciadorTokenJwt;
import com.bitequest.BiteQuest.repository.ComentarioRepository;
import com.bitequest.BiteQuest.repository.UsuarioRepository;
import com.bitequest.BiteQuest.usuario.ComentarioCreateRequestDto;
import com.bitequest.BiteQuest.usuario.UsuarioCreateRequestDto;
import com.bitequest.BiteQuest.usuario.UsuarioSimpleResponse;
import com.bitequest.BiteQuest.usuario.mapper.UsuarioMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.xml.bind.SchemaOutputResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private GerenciadorTokenJwt gerenciadorTokenJwt;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RestauranteService restauranteService;
    @Autowired
    private ComentarioRepository comentarioRepository;
    @Autowired
    private UsuarioMapper usuarioMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @Autowired
    public UsuarioService(AuthenticationManager authenticationManager, GerenciadorTokenJwt gerenciadorTokenJwt, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.gerenciadorTokenJwt = gerenciadorTokenJwt;
        this.usuarioRepository = usuarioRepository;
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Usuario adicionar(UsuarioCreateRequestDto u){
        String senhaCriptografia = passwordEncoder.encode(u.getSenha());

        Usuario novoUsuario = usuarioRepository.save(new Usuario(
                u.getNome(),
                u.getSobrenome(),
                u.getCpf(),
                u.getEmail(),
                u.getDataNascimento(),
                senhaCriptografia
        ));

        return novoUsuario;
    }


    public List<Usuario> todosUsuarios(){
        return usuarioRepository.findAll();
    }

    public Usuario usuarioPorId(Long id){
        return usuarioRepository.findById(id).orElseThrow(
                () -> new UsuarioNaoEncontradoException("Usuário não encontrado")
        );
    }

    public Optional<Usuario> usuarioPorEmail(String email){
        return usuarioRepository.findByEmail(email);
    }

    public Usuario editar(Long id, UsuarioSimpleResponse u){
        Usuario usuario = usuarioExiste(id);
        Usuario usuarioEditado = usuarioRepository.save(new Usuario(usuario.getId(), u.getNome(), u.getEmail()));
        return usuarioEditado;
    }



    public void deletarUsuario(Long id){
        usuarioRepository.deleteById(id);
    }

    public void atualizarEmail(Long id, String email){
        Optional<Usuario> usuario = usuarioRepository.findById(id);
        if(usuario.isPresent()){
            usuarioRepository.updateEmail(id,email);
        }
    }

    public Usuario usuarioExiste(Long id){
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(
                ()-> new UsuarioNaoEncontradoException("Usuário não encontrado")
        );
        return usuario;
    }

    public UsuarioTokenDto autenticar(UsuarioLoginDto usuarioLoginDto){
        final UsernamePasswordAuthenticationToken credentials = new UsernamePasswordAuthenticationToken(
                usuarioLoginDto.getEmail(),
                usuarioLoginDto.getSenha()
        );

        final Authentication authentication = authenticationManager.authenticate(credentials);

        Usuario usuarioAutenticado = usuarioRepository.findByEmail(usuarioLoginDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final String token = gerenciadorTokenJwt.generateToken(authentication);
        return UsuarioMapper.of(usuarioAutenticado, token);
    }

    @Transactional
    public Comentario adicionarComentario(Long idUsuario, Integer idRestaurante, ComentarioCreateRequestDto comentarioDto){
        // Busca o usuário pelo ID
        Usuario usuario = usuarioPorId(idUsuario);

        // Busca o restaurante pelo ID
        Restaurante restaurante = restauranteService.restaurantePorId(idRestaurante);

        // Cria um novo comentário
        Comentario novoComentario = new Comentario(usuario, restaurante, comentarioDto.getTexto());

        // Salva o comentário no banco de dados
        comentarioRepository.save(novoComentario);

        return novoComentario;
    }
}

