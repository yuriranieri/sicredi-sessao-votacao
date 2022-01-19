package br.com.sicredi.sessaovotacao.converter;

import br.com.sicredi.sessaovotacao.model.PautaEntity;
import br.com.sicredi.sessaovotacao.model.SessaoEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.com.sicredi.sessaovotacao.utils.PautaUtils.criarPautaResponseDTO;
import static br.com.sicredi.sessaovotacao.utils.SessaoUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessaoConverterTest {

    @InjectMocks
    private SessaoConverter converter;

    @Mock
    private PautaConverter pautaConverter;

    @Test
    void quandoConverterRequestDtoToEntity_retornoSucesso() {
        SessaoEntity entity = criarSessaoEntity();
        SessaoEntity entityConvertida = converter.requestDtoToEntity(criarSessaoRequestDTO());

        assertEquals(entity.getPauta().getId(), entityConvertida.getPauta().getId());
    }

    @Test
    void quandoConverterToResponseDto_retornoSucesso() {
        when(pautaConverter.toResponseDto(any(PautaEntity.class)))
                .thenReturn(criarPautaResponseDTO());

        assertEquals(criarSessaoResponseDTO(), converter.toResponseDto(criarSessaoEntity()));
    }

    @Test
    void quandoConverterToListResponseDto_retornoSucesso() {
        when(pautaConverter.toResponseDto(any(PautaEntity.class)))
                .thenReturn(criarPautaResponseDTO());

        assertEquals(criarListSessaoResponseDTO(), converter.toListResponseDto(criarListSessaoEntity()));
    }
}