package com.compass.finalproject.service;

import com.compass.finalproject.DTO.DenunciaDTO;
import com.compass.finalproject.DTO.DenunciaSaveFormDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.compass.finalproject.DTO.DetalhesDenunciaDTO;
import com.compass.finalproject.entity.Animais;
import com.compass.finalproject.entity.AnimaisEnum;
import com.compass.finalproject.entity.Denuncias;
import com.compass.finalproject.entity.Endereco;
import com.compass.finalproject.entity.StatusDenuncia;
import com.compass.finalproject.entity.Usuario;
import com.compass.finalproject.exceptions.ExceptionResponse;
import com.compass.finalproject.repository.AnimaisRepository;
import com.compass.finalproject.repository.DenunciaRepository;
import com.compass.finalproject.repository.EnderecoRepository;
import com.compass.finalproject.repository.OrgaoReponsavelRepository;
import com.compass.finalproject.repository.UsuarioRepository;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DenunciaServiceImpl implements DenunciaService {

	@Autowired
	DenunciaRepository denunciaRepository;

	@Autowired
	AnimaisRepository animaisRepository;

	@Autowired
	UsuarioRepository usuarioRepository;

	@Autowired
	EnderecoRepository enderecoRepository;

	@Autowired
	OrgaoReponsavelRepository orgaoReponsavelRepository;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public ResponseEntity<DenunciaDTO> save(DenunciaSaveFormDTO formDTO) {
		try {
			// Salva o Endereco no bando de dados
			Endereco enderecoDenuncia = this.enderecoRepository
					.save(modelMapper.map(formDTO.getEnderecoDenuncia(), Endereco.class));

			// Salva animal no banco
			Animais animalSalvo = this.animaisRepository.save(modelMapper.map(formDTO.getAnimal(), Animais.class));

			// Busca informações do usuário no banco
			Usuario usuario = this.usuarioRepository.getById(formDTO.getDenunciante());

			// Atribui informações de Endereço Tipo de Animal e Usuario a denúncia
			Denuncias denuncia = modelMapper.map(formDTO, Denuncias.class);
			denuncia.setEnderecoDenuncia(enderecoDenuncia);
			denuncia.setAnimal(animalSalvo);
			denuncia.setDenunciante(usuario);

			// Salva Denuncia no banco
			this.denunciaRepository.save(denuncia);
			return ResponseEntity.status(HttpStatus.CREATED).build();
		} catch (Exception e) {
			throw new ExceptionResponse(400, e.getMessage());
		}
	}

	@Override
	public ResponseEntity<List<DetalhesDenunciaDTO>> list(String tipoAnimal) {
			// Lista completa de denuncias
			if (tipoAnimal == null) {
				try {
					// Recupera todas as denuncias do banco de dados
					List<Denuncias> denuncias = denunciaRepository.findAll();

					// cria uma lista de detalhesDenunciaDTOs e adiciona todos os dados à ela
					List<DetalhesDenunciaDTO> detalhesDenunciaDTOs = new ArrayList<>();

					denuncias.forEach(de -> detalhesDenunciaDTOs.add(new DetalhesDenunciaDTO(de)));

					return ResponseEntity.ok(detalhesDenunciaDTOs);

				} catch (Exception e) {
					throw new ExceptionResponse(400);
				}
			}
			else {
					try {	
						tipoAnimal = StringUtils.capitalize(tipoAnimal); // Primeira letra da string maiúscula
						AnimaisEnum animalEnum = modelMapper.map(tipoAnimal, AnimaisEnum.class);

						// Busca os IDs de animais relacionados com o tipo informado:
						Optional<List<Animais>> animais = animaisRepository.findByTipoEquals(animalEnum);
						if (animais.get().size()>0){
							List<DetalhesDenunciaDTO> denuncias = new ArrayList<>();
							animais.get().forEach( animal -> {
								denuncias.add(new DetalhesDenunciaDTO(
									denunciaRepository.findByAnimalIdEquals(animal.getId()).get()));
							});
							return ResponseEntity.ok(denuncias);
						}
						return  ResponseEntity.notFound().build();
					} catch (Exception e) {
						throw new ExceptionResponse(400, e.getMessage());
					}
			}
	}

	@Override
	public ResponseEntity<DenunciaDTO> update(int id, DenunciaSaveFormDTO formDTO) {
		try {
			Optional<Denuncias> denuncia = this.denunciaRepository.findById(id);
			if (denuncia.isPresent()) {
				Optional<Endereco> endereco = this.enderecoRepository.findById(denuncia.get().getEnderecoDenuncia().getId());
				Optional<Animais> animal = this.animaisRepository.findById(denuncia.get().getAnimal().getId());

				// Verifica se o status está em como "Aberto", caso contrário o usuário
				// não pode alterar a denúncia
				if (denuncia.get().getStatus().equals(StatusDenuncia.Aberto)) {
					denuncia.get().setMensagem(formDTO.getMensagem());
					// Alterando Endereço
					endereco.get().setBairro(formDTO.getEnderecoDenuncia().getBairro());
					endereco.get().setCep(formDTO.getEnderecoDenuncia().getCep());
					endereco.get().setCidade(formDTO.getEnderecoDenuncia().getCidade());
					endereco.get().setComplemento(formDTO.getEnderecoDenuncia().getComplemento());
					endereco.get().setEstado(formDTO.getEnderecoDenuncia().getEstado());
					endereco.get().setLogradouro(formDTO.getEnderecoDenuncia().getLogradouro());
					endereco.get().setNumero(formDTO.getEnderecoDenuncia().getNumero());

					// Alterando Animal
					animal.get().setCor(formDTO.getAnimal().getCor());
					animal.get().setRaca(formDTO.getAnimal().getRaca());
					animal.get().setTipo(formDTO.getAnimal().getTipo());

					// Atualizando denúncia
					denuncia.get().setEnderecoDenuncia(endereco.get());
					denuncia.get().setAnimal(animal.get());

					return ResponseEntity.ok(modelMapper.map(denuncia.get(), DenunciaDTO.class));
				}
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			throw new ExceptionResponse(400,"Erro no preenchimento do formulário");
		}
	}

	@Override
	public ResponseEntity<?> delete(int id) {
		try {
			Optional<Denuncias> optional = denunciaRepository.findById(id);
			if (optional.isPresent()) {
				denunciaRepository.deleteById(id);
				return ResponseEntity.ok().build();
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			throw new ExceptionResponse(400, "Não foi possível deletar a denúncia de ID "+id+" devido a um erro no formulario.");
		}
	}

	@Override
	public ResponseEntity<DenunciaDTO> getDenuncia(int id) {
		try {
			Optional<Denuncias> dOptional = denunciaRepository.findById(id);
			if(dOptional.isPresent()){
				return ResponseEntity.ok(modelMapper.map(dOptional.get(), DenunciaDTO.class));
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			throw new ExceptionResponse(400);
		}	
	}

	@Override
	public ResponseEntity<StatusDenuncia> listStatus(int id) {
		try {
			Optional<Denuncias> dOptional = denunciaRepository.findById(id);
			if(dOptional.isPresent()){

				return ResponseEntity.ok(dOptional.get().getStatus());
			}
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			throw new ExceptionResponse(400);
		}
	}
}
