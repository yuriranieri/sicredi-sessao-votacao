package br.com.sicredi.sessaovotacao.business;

import br.com.sicredi.sessaovotacao.dto.VotoRequestDTO;
import br.com.sicredi.sessaovotacao.dto.VotoResponseDTO;
import br.com.sicredi.sessaovotacao.exception.ErrorBusinessException;
import br.com.sicredi.sessaovotacao.mapper.VotoMapper;
import br.com.sicredi.sessaovotacao.model.AssociadoEntity;
import br.com.sicredi.sessaovotacao.model.SessaoEntity;
import br.com.sicredi.sessaovotacao.model.VotoEntity;
import br.com.sicredi.sessaovotacao.model.VotoPK;
import br.com.sicredi.sessaovotacao.service.AssociadoService;
import br.com.sicredi.sessaovotacao.service.SessaoService;
import br.com.sicredi.sessaovotacao.service.VotoService;
import br.com.sicredi.sessaovotacao.userapi.client.UserClient;
import br.com.sicredi.sessaovotacao.userapi.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static br.com.sicredi.sessaovotacao.utils.AssociadoUtils.criarAssociadoEntity;
import static br.com.sicredi.sessaovotacao.utils.SessaoUtils.criarSessaoEntity;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarListVotoEntity;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarListVotoResponseDTO;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarPageVotoEntity;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarPageVotoResponseDTO;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarUserDTO;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarVotoEntity;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarVotoRelatorioDTO;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarVotoRequestDTO;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.criarVotoResponseDTO;
import static br.com.sicredi.sessaovotacao.utils.VotoUtils.getPageable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotoBusinessTest {

    @InjectMocks
    private VotoBusiness business;

    @Mock
    private VotoMapper mapper;

    @Mock
    private VotoService service;

    @Mock
    private UserClient userClient;

    @Mock
    private AssociadoService associadoService;

    @Mock
    private SessaoService sessaoService;

    @Test
    void quandoSalvarVoto_comAssociadoQueJaVoltou_retornaErro() {
        VotoRequestDTO requestDTO = criarVotoRequestDTO();
        VotoEntity entity = criarVotoEntity();
        VotoPK votoPK = entity.getId();

        when(mapper.requestDtoToEntity(any(VotoRequestDTO.class)))
                .thenReturn(entity);
        when(service.buscarPorId(any(VotoPK.class)))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> business.salvar(requestDTO))
                .isInstanceOf(ErrorBusinessException.class)
                .hasMessage(String.format("Associado %d já votou na sessão %d",
                        votoPK.getIdAssociado(),
                        votoPK.getIdSessao()));
    }

    @Test
    void quandoSalvarVoto_comAssociadoComStatusUnableToVote_retornaErro() {
        VotoRequestDTO requestDTO = criarVotoRequestDTO();
        VotoEntity entity = criarVotoEntity();
        AssociadoEntity associadoEntity = criarAssociadoEntity();
        UserDTO userDTO = criarUserDTO();
        userDTO.setStatus("UNABLE_TO_VOTE");

        when(mapper.requestDtoToEntity(any(VotoRequestDTO.class)))
                .thenReturn(entity);
        when(service.buscarPorId(any(VotoPK.class)))
                .thenReturn(Optional.empty());
        when(associadoService.buscarPorId(anyLong()))
                .thenReturn(associadoEntity);
        when(userClient.carregarEntidade(anyString()))
                .thenReturn(userDTO);

        assertThatThrownBy(() -> business.salvar(requestDTO))
                .isInstanceOf(ErrorBusinessException.class)
                .hasMessage(String.format("Associado com o cpf %s não pode votar", associadoEntity.getCpf()));
    }

    @Test
    void quandoSalvarVoto_comSessaoExpirada_retornaErro() {
        VotoRequestDTO requestDTO = criarVotoRequestDTO();
        VotoEntity entity = criarVotoEntity();
        UserDTO userDTO = criarUserDTO();
        SessaoEntity sessaoEntity = criarSessaoEntity();
        sessaoEntity.setDataInicio(LocalDateTime.now().minusMinutes(10));
        sessaoEntity.setDataFinal(LocalDateTime.now().minusMinutes(5));

        when(mapper.requestDtoToEntity(any(VotoRequestDTO.class)))
                .thenReturn(entity);
        when(service.buscarPorId(any(VotoPK.class)))
                .thenReturn(Optional.empty());
        when(associadoService.buscarPorId(anyLong()))
                .thenReturn(criarAssociadoEntity());
        when(userClient.carregarEntidade(anyString()))
                .thenReturn(userDTO);
        when(sessaoService.buscarPorId(anyLong()))
                .thenReturn(sessaoEntity);

        assertThatThrownBy(() -> business.salvar(requestDTO))
                .isInstanceOf(ErrorBusinessException.class)
                .hasMessage("Sessão foi expirada");
    }

    @Test
    void quandoSalvarVoto_retornaSucesso() {
        VotoEntity entity = criarVotoEntity();
        UserDTO userDTO = criarUserDTO();
        VotoResponseDTO responseDTO = criarVotoResponseDTO();
        SessaoEntity sessaoEntity = criarSessaoEntity();
        sessaoEntity.setDataInicio(LocalDateTime.now());
        sessaoEntity.setDataFinal(LocalDateTime.now().plusMinutes(5));

        when(mapper.requestDtoToEntity(any(VotoRequestDTO.class)))
                .thenReturn(entity);
        when(service.buscarPorId(any(VotoPK.class)))
                .thenReturn(Optional.empty());
        when(associadoService.buscarPorId(anyLong()))
                .thenReturn(criarAssociadoEntity());
        when(userClient.carregarEntidade(anyString()))
                .thenReturn(userDTO);
        when(sessaoService.buscarPorId(anyLong()))
                .thenReturn(sessaoEntity);
        when(service.salvar(any(VotoEntity.class)))
                .thenReturn(entity);
        when(mapper.toResponseDto(any(VotoEntity.class)))
                .thenReturn(responseDTO);

        assertThat(business.salvar(criarVotoRequestDTO()))
                .isNotNull()
                .isEqualTo(responseDTO);
    }

    @Test
    void quandoListarVoto_retornaSucesso() {
        when(service.listar(any()))
                .thenReturn(criarPageVotoEntity());
        when(mapper.toPageResponseDto(any()))
                .thenReturn(criarPageVotoResponseDTO());

        assertThat(business.listar(getPageable()))
                .isNotNull()
                .hasSize(1);
    }

    @Test
    void quandoListarVotoPorIdSessao_retornaSucesso() {
        Long idSessao = anyLong();

        when(service.listarPorIdSessao(idSessao))
                .thenReturn(criarListVotoEntity());
        when(mapper.toListResponseDto(anyList()))
                .thenReturn(criarListVotoResponseDTO());

        assertThat(business.listarPorIdSessao(idSessao))
                .isNotNull()
                .hasSize(1);
    }

    @Test
    void quandoListarVotoPorIdAssociado_retornaSucesso() {
        Long idSessao = anyLong();

        when(service.listarPorIdAssociado(idSessao, any()))
                .thenReturn(criarPageVotoEntity());
        when(mapper.toPageResponseDto(any()))
                .thenReturn(criarPageVotoResponseDTO());

        assertThat(business.listarPorIdAssociado(idSessao, getPageable()))
                .isNotNull()
                .hasSize(1);
    }

    @Test
    void quandoCalcularVotosDaSessao_comSessaoEmAndamento_retornaErro() {
        Long idSessao = anyLong();
        SessaoEntity sessaoEntity = criarSessaoEntity();
        sessaoEntity.setDataInicio(LocalDateTime.now().minusMinutes(1));
        sessaoEntity.setDataFinal(LocalDateTime.now().plusMinutes(3));

        when(sessaoService.buscarPorId(idSessao))
                .thenReturn(sessaoEntity);

        assertThatThrownBy(() -> business.calcularVotosDaSessao(idSessao))
                .isInstanceOf(ErrorBusinessException.class)
                .hasMessage("Sessão em andamento espere a finalização dela");
    }

    @Test
    void quandoCalcularVotosDaSessao_retornaSucesso() {
        Long idSessao = anyLong();
        SessaoEntity sessaoEntity = criarSessaoEntity();
        sessaoEntity.setDataInicio(LocalDateTime.now().minusMinutes(10));
        sessaoEntity.setDataFinal(LocalDateTime.now().minusMinutes(5));

        when(sessaoService.buscarPorId(idSessao))
                .thenReturn(sessaoEntity);
        when(service.listarPorIdSessao(idSessao))
                .thenReturn(criarListVotoEntity());
        when(mapper.toListResponseDto(anyList()))
                .thenReturn(criarListVotoResponseDTO());

        assertEquals(criarVotoRelatorioDTO(), business.calcularVotosDaSessao(idSessao));
    }
}