from pathlib import Path

board = Path('app/src/main/java/com/ryusgua/app/LiuYaoBoard.java')
s = board.read_text(encoding='utf-8')
s = s.replace(',void,trueVoid,', ',xunVoid,trueVoid,')
s = s.replace('if(void)', 'if(xunVoid)')
s = s.replace('.void', '.xunVoid')
board.write_text(s, encoding='utf-8')

repo = Path('app/src/main/java/com/ryusgua/app/LiuYaoKnowledgeRepository.java')
s = repo.read_text(encoding='utf-8').replace('.void', '.xunVoid')
repo.write_text(s, encoding='utf-8')

print('renamed reserved field void -> xunVoid')
