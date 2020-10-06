.486
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\masm32.inc
include C:\masm32\include\gdi32.inc
include C:\masm32\include\user32.inc

include C:\masm32\include\windows.inc
include C:\masm32\macros\macros.asm

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\masm32.lib
includelib C:\masm32\lib\gdi32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data

.code

start:

invoke main
invoke ExitProcess, 0

main PROC
push 3
pop eax
neg eax
push eax

push 2
push 1

pop BL
pop AX
div BL
push AL

push 4

pop BL
pop AX
div BL
push AL


pop BL
pop AX
div BL
push AL


pop eax ;here is the result

ret
main ENDP
END start
