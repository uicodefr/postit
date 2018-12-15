package com.uicode.postit.postitserver.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.google.common.collect.Streams;
import com.uicode.postit.postitserver.dao.postit.IBoardDao;
import com.uicode.postit.postitserver.dao.postit.IPostitNoteDao;
import com.uicode.postit.postitserver.dto.postit.BoardDto;
import com.uicode.postit.postitserver.dto.postit.PostitNoteDto;
import com.uicode.postit.postitserver.entity.postit.Board;
import com.uicode.postit.postitserver.entity.postit.PostitNote;
import com.uicode.postit.postitserver.mapper.postit.BoardMapper;
import com.uicode.postit.postitserver.mapper.postit.PostitNoteMapper;
import com.uicode.postit.postitserver.service.IGlobalService;
import com.uicode.postit.postitserver.service.IPostitNoteService;
import com.uicode.postit.postitserver.utils.CheckDataUtils;
import com.uicode.postit.postitserver.utils.exception.FunctionnalException;
import com.uicode.postit.postitserver.utils.exception.InvalidDataException;
import com.uicode.postit.postitserver.utils.exception.NotFoundException;
import com.uicode.postit.postitserver.utils.parameter.ParameterConst;
import com.uicode.postit.postitserver.utils.parameter.ParameterUtils;


@Service
@Transactional
public class PostitNoteServiceImpl implements IPostitNoteService {

    private static final Logger LOGGER = LogManager.getLogger(PostitNoteServiceImpl.class);

    @Autowired
    private IBoardDao boardDao;

    @Autowired
    private IPostitNoteDao postitNoteDao;

    @Autowired
    private IGlobalService globalService;

    @Override
    public List<BoardDto> getBoardList() {
        Iterable<Board> boardIterable = boardDao.findAll(new Sort(Direction.ASC, "id"));
        return Streams.stream(boardIterable).map(BoardMapper.INSTANCE::toDto).collect(Collectors.toList());
    }

    @Override
    public BoardDto saveBoard(Long boardId, BoardDto boardDto) throws NotFoundException, FunctionnalException {
        Board board = null;

        if (boardId == null) {
            // Creation
            Optional<String> maxBoardParameter = globalService.getParameterValue(ParameterConst.BOARD_MAX);
            Long maxBoard = ParameterUtils.getLong(maxBoardParameter, 0l);
            if (boardDao.count() > maxBoard) {
                throw new FunctionnalException("Max Board achieved : creation is blocked");
            }

            board = new Board();

        } else {
            // Update
            Optional<Board> boardOpt = boardDao.findById(boardId);
            board = boardOpt.orElseThrow(() -> new NotFoundException("Board"));
        }

        board.setName(boardDto.getName());

        return BoardMapper.INSTANCE.toDto(boardDao.save(board));
    }

    @Override
    public void deleteBoard(Long boardId) {
        Optional<Board> boardOpt = boardDao.findById(boardId);
        if (!boardOpt.isPresent()) {
            LOGGER.warn("Board not found for deletion, id = %s", boardId);
            return;
        }

        Board board = boardOpt.get();
        board.getNoteList().forEach(postitNoteDao::delete);
        boardDao.delete(board);
    }

    @Override
    public List<PostitNoteDto> getNoteList(Long boardId) {
        Iterable<PostitNote> noteIterable = postitNoteDao.findByBoardIdOrderByOrderNum(boardId);
        return Streams.stream(noteIterable).map(PostitNoteMapper.INSTANCE::toDto).collect(Collectors.toList());
    }

    @Override
    public PostitNoteDto getNote(Long noteId) throws NotFoundException {
        Optional<PostitNote> noteOpt = postitNoteDao.findById(noteId);
        return PostitNoteMapper.INSTANCE.toDto(noteOpt.orElseThrow(() -> new NotFoundException("Note")));
    }

    @Override
    public PostitNoteDto saveNote(Long noteId, PostitNoteDto noteDto)
            throws NotFoundException, InvalidDataException, FunctionnalException {
        PostitNote note = null;

        if (noteId == null) {
            // Creation
            CheckDataUtils.checkNotNull("boardId", noteDto.getBoardId());
            Optional<String> maxNoteParameter = globalService.getParameterValue(ParameterConst.NOTE_MAX);
            Long maxNote = ParameterUtils.getLong(maxNoteParameter, 0l);
            if (postitNoteDao.countByBoardId(noteDto.getBoardId()) > maxNote) {
                throw new FunctionnalException("Max Postit Note achieved : creation is blocked");
            }

            note = new PostitNote();
            note.setOrderNum(postitNoteDao.getMaxOrderForByBoardId(noteDto.getBoardId()) + 1);

        } else {
            // Update
            Optional<PostitNote> noteOpt = postitNoteDao.findById(noteId);
            note = noteOpt.orElseThrow(() -> new NotFoundException("PostitNote"));
            if (noteDto.getOrderNum() != null) {
                reorderBoard(note, noteDto);
            }
        }

        if (noteDto.getBoardId() != null) {
            Optional<Board> boardOpt = boardDao.findById(noteDto.getBoardId());
            note.setBoard(boardOpt.orElseThrow(() -> new InvalidDataException("BoardId")));
        }
        PostitNoteMapper.INSTANCE.updateEntity(noteDto, note);

        return PostitNoteMapper.INSTANCE.toDto(postitNoteDao.save(note));
    }

    @Override
    public void reorderBoard(PostitNote noteToChange, PostitNoteDto noteChangeDto) {
        if (noteChangeDto.getOrderNum() < 1) {
            noteChangeDto.setOrderNum(1);
        } else if (noteChangeDto.getOrderNum() > noteToChange.getBoard().getNoteList().size()) {
            noteChangeDto.setOrderNum(noteToChange.getBoard().getNoteList().size());
        }

        Integer orderNum = 1;
        for (PostitNote noteOfBoard : noteToChange.getBoard().getNoteList()) {
            if (noteOfBoard.getId().equals(noteToChange.getId())) {
                noteToChange.setOrderNum(noteChangeDto.getOrderNum());
            } else {
                if (noteChangeDto.getOrderNum().equals(orderNum)) {
                    orderNum++;
                }
                noteOfBoard.setOrderNum(orderNum++);
            }
        }
    }

    @Override
    public void deleteNote(Long noteId) {
        postitNoteDao.deleteById(noteId);
    }

}
